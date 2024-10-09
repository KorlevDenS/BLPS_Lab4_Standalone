package org.korolev.dens.blps_lab4_standalone.services;

import jakarta.annotation.Nullable;
import org.korolev.dens.blps_lab4_standalone.entites.*;
import org.korolev.dens.blps_lab4_standalone.exceptions.*;
import org.korolev.dens.blps_lab4_standalone.repositories.*;
import org.korolev.dens.blps_lab4_standalone.requests.StatsMessage;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.util.*;

@Service
public class TopicService {

    private final TransactionTemplate transactionTemplate;
    private final ClientRepository clientRepository;
    private final TopicRepository topicRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final RatingRepository ratingRepository;
    private final ChapterRepository chapterRepository;
    private final ImageRepository imageRepository;
    private final MessageProducer messageProducer;


    public TopicService(PlatformTransactionManager platformTransactionManager, ClientRepository clientRepository,
                        TopicRepository topicRepository, SubscriptionRepository subscriptionRepository,
                        RatingRepository ratingRepository, ChapterRepository chapterRepository, ImageRepository imageRepository, MessageProducer messageProducer) {
        this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
        this.clientRepository = clientRepository;
        this.topicRepository = topicRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.ratingRepository = ratingRepository;
        this.chapterRepository = chapterRepository;
        this.imageRepository = imageRepository;
        this.transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
        this.messageProducer = messageProducer;
    }

    public List<Topic> findAllByChapter(Integer chapterId) throws ForumObjectNotFoundException {
        if (chapterRepository.findById(chapterId).isEmpty()) {
            throw new ForumObjectNotFoundException("No chapter with id " + chapterId);
        }
        return topicRepository.getAllByChapter(chapterId);
    }

    public Topic findTopicById(Integer topicId) throws ForumObjectNotFoundException {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Optional<Topic> optionalTopic = topicRepository.findById(topicId);
        if (optionalTopic.isEmpty()) {
            throw new ForumObjectNotFoundException("No topic with id " + topicId);
        }
        if (userDetails == null) {
            messageProducer.sendMessage(new StatsMessage(optionalTopic.get().getId(), "", "watch",
                    optionalTopic.get().getOwner().getLogin()));
        } else {
            messageProducer.sendMessage(new StatsMessage(optionalTopic.get().getId(), userDetails.getUsername(),
                    "watch", optionalTopic.get().getOwner().getLogin()));
        }
        return optionalTopic.get();
    }

    public Resource getImageFromDisk(String imageUrl) throws ImageAccessException {
        try {
            String storage = System.getenv("PHOTO_STORAGE");
            Path filepath = Paths.get(storage).resolve(imageUrl);
            Resource resource = new UrlResource(filepath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new ImageAccessException("Image does not exist", HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            throw new ImageAccessException("Cased by " + e.getClass() + ": " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasRole('MODER')")
    public void delete(Integer topicId) throws ForumException {
        TransactionExceptionKeeper keeper = new TransactionExceptionKeeper();
        transactionTemplate.execute(status -> {
            Pair<Path, Map<Path, Path>> backup = null;
            Topic topic;
            try {
                topic = topicRepository.findById(topicId)
                        .orElseThrow(() -> new NoSuchElementException("Topic " + topicId + " not found"));
            } catch (NoSuchElementException e) {
                keeper.setEx(new ForumObjectNotFoundException(e.getMessage()));
                return null;
            }
            Client topicOwner = topic.getOwner();
            try {
                try {
                    backup = makeImgBackup(topic);
                } catch (ImagesBackupException e) {
                    status.setRollbackOnly();
                    keeper.setEx(e);
                    return null;
                }
                try {
                    for (Image image : topic.getImages()) {
                        imageRepository.deleteById(image.getId());
                    }
                } catch (Exception e) {
                    status.setRollbackOnly();
                    deleteReservedFiles(backup.getFirst());
                    keeper.setEx(new ForumException("Could not delete images from db"));
                    return null;
                }
                try {
                    topicRepository.deleteById(topicId);
                } catch (Exception e) {
                    status.setRollbackOnly();
                    deleteReservedFiles(backup.getFirst());
                    keeper.setEx(new ForumException("Could not delete topic from db"));
                    return null;
                }
                for (Image image : topic.getImages()) {
                    String imgLink = image.getLink();
                    File file = new File(imgLink);
                    if (!file.delete()) {
                        restoreDeletedFiles(backup.getSecond());
                        status.setRollbackOnly();
                        deleteReservedFiles(backup.getFirst());
                        keeper.setEx(new ForumException("Could not delete image from disk"));
                        return null;
                    }
                }
            } catch (Exception e) {
                status.setRollbackOnly();
                if (backup != null) deleteReservedFiles(backup.getFirst());
                keeper.setEx(new ForumException("Topic with id " + topicId + " was not deleted"));
                return null;
            }
            deleteReservedFiles(backup.getFirst());
            messageProducer.sendMessage(new StatsMessage(topicId, "", "delete",
                    topicOwner.getLogin()));
            return null;
        });
        keeper.throwIfSet();
    }

    private void restoreDeletedFiles(Map<Path, Path> imgMap) {
        imgMap.forEach((source, reserved) -> {
            if (!source.toFile().exists()) {
                try {
                    Files.copy(reserved, source);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private Pair<Path, Map<Path, Path>> makeImgBackup(Topic topic) throws ImagesBackupException {
        String reservedDirName = Paths.get(System.getenv("PHOTO_STORAGE"))
                .resolve("_" + topic.getId()).toString();
        File reservedDir = new File(reservedDirName);
        if (!reservedDir.mkdir()) {
            throw new ImagesBackupException("Could not create backup directory");
        }
        Map<Path, Path> imgMap = new HashMap<>();
        Path path1 = Paths.get(reservedDirName);
        for (Image image : topic.getImages()) {
            String imgName = Paths.get(image.getLink()).getFileName().toString();
            Path reservedImg = path1.resolve(imgName);
            try {
                Path reservedPath = Files.copy(new File(image.getLink()).toPath(), reservedImg);
                imgMap.put(new File(image.getLink()).toPath(), reservedPath);
            } catch (IOException e) {
                if (!deleteReservedFiles(reservedDir.toPath())) {
                    throw new ImagesBackupException("Creating backup problem: Not possible to to delete "
                            + reservedDirName);
                }
                throw new ImagesBackupException("Creating backup problem: " + e.getMessage());
            }
        }
        return Pair.of(path1, imgMap);
    }

    private boolean deleteReservedFiles(Path folderPath) {
        try {
            Files.walkFileTree(folderPath, EnumSet.of(FileVisitOption.FOLLOW_LINKS),
                    Integer.MAX_VALUE, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @PreAuthorize("hasRole('USER')")
    public Topic add(Topic topic, Integer chapterId, @Nullable MultipartFile img1,
                                 @Nullable MultipartFile img2, @Nullable MultipartFile img3) throws ForumException {
        String login = ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .getUsername();
        MultipartFile[] multipartFiles = {img1, img2, img3};
        if (!validateImgList(Arrays.asList(multipartFiles))) {
            throw new ImageAccessException("Неверный формат файла. Допустимы png и jpg", HttpStatus.BAD_REQUEST);
        }
        TransactionExceptionKeeper keeper = new TransactionExceptionKeeper();
        Topic t = transactionTemplate.execute(status -> {
            Topic addedTopic;
            try {
                Optional<Chapter> optionalChapter = chapterRepository.findById(chapterId);
                Optional<Client> optionalClient = clientRepository.findByLogin(login);
                if (optionalClient.isEmpty()) {
                    keeper.setEx(new ForbiddenActionException("Illegal access to resource: no such client in database"));
                    return null;
                } else if (optionalChapter.isEmpty()) {
                    keeper.setEx(new ForumObjectNotFoundException("No chapter with id " + chapterId));
                    return null;
                }
                topic.setOwner(optionalClient.get());
                topic.setChapter(optionalChapter.get());
                try {
                    addedTopic = topicRepository.save(topic);
                } catch (Exception e) {
                    status.setRollbackOnly();
                    keeper.setEx(new ForumException("Error occurred when saving topic"));
                    return null;
                }
                List<String> imgLinks = new ArrayList<>();
                try {
                    if (img1 != null) {
                        imgLinks.add(uploadImage(img1, 1, addedTopic.getId()).toString());
                    }
                    if (img2 != null) {
                        imgLinks.add(uploadImage(img2, 2, addedTopic.getId()).toString());
                    }
                    if (img3 != null) {
                        imgLinks.add(uploadImage(img3, 3, addedTopic.getId()).toString());
                    }
                } catch (IOException e) {
                    for (String imgLink : imgLinks) {
                        File imgFile = new File(imgLink);
                        if (!imgFile.delete()) {
                            System.err.println("Useless file " + imgLink + " was not deleted automatically!");
                        }
                    }
                    status.setRollbackOnly();
                    keeper.setEx(new ForumLogicException("Unable to save topic because we cannot upload the image",
                            HttpStatus.CONFLICT));
                    return null;
                }
                for (String imgLink : imgLinks) {
                    Image image = new Image();
                    image.setTopic(addedTopic);
                    image.setLink(imgLink);
                    image.setCreated(LocalDate.now());
                    try {
                        imageRepository.save(image);
                    } catch (Exception e) {
                        status.setRollbackOnly();
                        keeper.setEx(new ForumException("Error occurred when saving image"));
                        return null;
                    }
                }
            } catch (Exception e) {
                status.setRollbackOnly();
                keeper.setEx(new ForumException("Topic was not added"));
                return null;
            }
            messageProducer.sendMessage(new StatsMessage(addedTopic.getId(), "", "add", login));
            return addedTopic;
        });
        keeper.throwIfSet();
        return t;
    }

    private Path uploadImage(MultipartFile img, int i, int id) throws IOException {
        String storage = System.getenv("PHOTO_STORAGE");
        Path imagePath = Paths.get(storage).resolve("t" + id + "_i" + i + img.getOriginalFilename());
        return Files.write(imagePath, img.getBytes());
    }

    private boolean validateImgList(List<MultipartFile> perhapsImages) {
        for (MultipartFile perhapsImg: perhapsImages) {
            if (perhapsImg != null) {
                try {
                    if (!validateImageFormat(perhapsImg)) {
                        return false;
                    }
                } catch (IOException e) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean validateImageFormat(MultipartFile perhapsImg) throws IOException {
        String originalFileName = perhapsImg.getOriginalFilename();
        if (!(originalFileName != null
                && (originalFileName.toLowerCase().endsWith(".png") || originalFileName.toLowerCase().endsWith(".jpg"))
        )) {
            return false;
        }
        byte[] header = perhapsImg.getBytes();
        if (header.length > 8 &&
                header[0] == (byte) 0x89 && header[1] == (byte) 0x50 &&
                header[2] == (byte) 0x4E && header[3] == (byte) 0x47 &&
                header[4] == (byte) 0x0D && header[5] == (byte) 0x0A &&
                header[6] == (byte) 0x1A && header[7] == (byte) 0x0A) {
            return true;
        }
        return header.length > 2 &&
                header[0] == (byte) 0xFF && header[1] == (byte) 0xD8;
    }

    @PreAuthorize("hasRole('USER')")
    public Subscription subscribe(Integer topicId) throws ForbiddenActionException, ForumObjectNotFoundException,
            ForumLogicException {
        String login = ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .getUsername();
        Optional<Client> optionalClient = clientRepository.findByLogin(login);
        Optional<Topic> optionalTopic = topicRepository.findById(topicId);
        if (optionalClient.isEmpty()) {
            throw new ForbiddenActionException("Illegal access to resource: no such client in database");
        } else if (optionalTopic.isEmpty()) {
            throw new ForumObjectNotFoundException("Topic " + topicId + " does not exist");
        }
        Optional<Subscription> optionalSubscription =
                subscriptionRepository.findByClientAndTopic(optionalClient.get(), optionalTopic.get());
        if (optionalSubscription.isPresent()) {
            throw new ForumLogicException("You have already subscribed for this topic", HttpStatus.ALREADY_REPORTED);
        } else {
            Subscription subscription = new Subscription();
            SubscriptionId subscriptionId = new SubscriptionId();
            subscriptionId.setClient(optionalClient.get().getId());
            subscriptionId.setTopic(topicId);
            subscription.setId(subscriptionId);
            subscription.setClient(optionalClient.get());
            subscription.setTopic(optionalTopic.get());
            return subscriptionRepository.save(subscription);
        }
    }

    @PreAuthorize("hasRole('USER')")
    public void unsubscribe(Integer topicId) throws ForumException {
        String login = ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .getUsername();
        TransactionExceptionKeeper keeper = new TransactionExceptionKeeper();
        transactionTemplate.execute(status -> {
            try {
                Client client = clientRepository.findByLogin(login).orElseThrow(Exception::new);
                Topic topic = topicRepository.findById(topicId)
                        .orElseThrow(() -> new NoSuchElementException("Topic not found"));
                subscriptionRepository.findByClientAndTopic(client, topic)
                        .orElseThrow(() -> new NoSuchElementException("Subscription not found"));
                subscriptionRepository.deleteByClientAndTopic(client, topic);
            } catch (NoSuchElementException e) {
                status.setRollbackOnly();
                keeper.setEx(new ForumObjectNotFoundException(e.getMessage()));
                return null;
            } catch (Exception e) {
                status.setRollbackOnly();
                keeper.setEx(new ForumException("Unsubscription failed"));
                return null;
            }
            return null;
        });
        keeper.throwIfSet();
    }

    @PreAuthorize("hasRole('USER')")
    public Topic update(Topic topic) throws ForumException {
        String login = ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .getUsername();
        TransactionExceptionKeeper keeper = new TransactionExceptionKeeper();
        Topic updated = transactionTemplate.execute(status -> {
            Topic t;
            try {
                t = topicRepository.findById(topic.getId())
                        .orElseThrow(() -> new NoSuchElementException("Topic " + topic.getId() + " not found"));
                if (!(t.getOwner().getLogin().equals(login))) {
                    keeper.setEx(new ForbiddenActionException("Topic with id " + topic.getId() + " is not yours"));
                    return null;
                }
                if (!Objects.equals(t.getTitle(), topic.getTitle())) {
                    topicRepository.updateTitle(t.getId(), topic.getTitle(), login);
                }
                if (!Objects.equals(t.getText(), topic.getText())) {
                    topicRepository.updateText(t.getId(), topic.getText(), login);
                }
            } catch (NoSuchElementException e) {
                status.setRollbackOnly();
                keeper.setEx(new ForumObjectNotFoundException(e.getMessage()));
                return null;
            } catch (Exception e) {
                status.setRollbackOnly();
                keeper.setEx(new ForumLogicException("Update failed", HttpStatus.CONFLICT));
                return null;
            }
            return t;
        });
        keeper.throwIfSet();
        return updated;
    }

    @PreAuthorize("hasRole('USER')")
    public Rating rate(Rating rating, Integer topicId) throws ForumException {
        String login = ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .getUsername();
        TransactionExceptionKeeper keeper = new TransactionExceptionKeeper();
        Rating r = transactionTemplate.execute(status -> {
            try {
                Client client = clientRepository.findByLogin(login).orElseThrow(Exception::new);
                Topic topic = topicRepository.findById(topicId)
                        .orElseThrow(() -> new NoSuchElementException("Topic " + topicId + " not found"));
                Optional<Rating> optionalRating = ratingRepository.findRatingByCreatorAndTopic(client, topic);
                if (optionalRating.isPresent()) {
                    ratingRepository.updateRatingByClientAndTopic(login, rating.getRating(), topicId);
                    messageProducer.sendMessage(new StatsMessage(topicId, login, rating.getRating().toString(),
                            topic.getOwner().getLogin()));
                    return optionalRating.get();
                } else {
                    rating.setCreator(client);
                    rating.setTopic(topic);
                    Rating addedRating = ratingRepository.save(rating);
                    messageProducer.sendMessage(new StatsMessage(topicId, login, rating.getRating().toString(),
                            topic.getOwner().getLogin()));
                    return addedRating;
                }
            } catch (NoSuchElementException e) {
                status.setRollbackOnly();
                keeper.setEx(new ForumObjectNotFoundException(e.getMessage()));
                return null;
            } catch (Exception e) {
                status.setRollbackOnly();
                keeper.setEx(new ForumException("Rating topic failed"));
                return null;
            }
        });
        keeper.throwIfSet();
        return r;
    }

    @PreAuthorize("hasRole('USER')")
    public List<Rating> findTopicRatings(Integer topicId) throws ForumObjectNotFoundException {
        Optional<Topic> optionalTopic = topicRepository.findById(topicId);
        if (optionalTopic.isEmpty()) {
            throw new ForumObjectNotFoundException("Topic " + topicId + " does not exist");
        }
        return ratingRepository.findAllByTopic(optionalTopic.get());
    }

}
