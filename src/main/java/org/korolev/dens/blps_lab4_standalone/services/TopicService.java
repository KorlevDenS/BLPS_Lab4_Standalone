package org.korolev.dens.blps_lab4_standalone.services;

import jakarta.annotation.Nullable;
import org.korolev.dens.blps_lab4_standalone.entites.*;
import org.korolev.dens.blps_lab4_standalone.exceptions.*;
import org.korolev.dens.blps_lab4_standalone.repositories.*;
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
    private final ApprovalRepository approvalRepository;


    public TopicService(PlatformTransactionManager platformTransactionManager, ClientRepository clientRepository,
                        TopicRepository topicRepository, SubscriptionRepository subscriptionRepository,
                        RatingRepository ratingRepository, ChapterRepository chapterRepository,
                        ImageRepository imageRepository, ApprovalRepository approvalRepository) {
        this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
        this.clientRepository = clientRepository;
        this.topicRepository = topicRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.ratingRepository = ratingRepository;
        this.chapterRepository = chapterRepository;
        this.imageRepository = imageRepository;
        this.transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
        this.approvalRepository = approvalRepository;
    }

    public List<Topic> findAllByChapter(Integer chapterId) throws ForumObjectNotFoundException {
        if (chapterRepository.findById(chapterId).isEmpty()) {
            throw new ForumObjectNotFoundException("No chapter with id " + chapterId);
        }
        return topicRepository.getAllApprovedByChapter(chapterId);
    }

    @PreAuthorize("hasRole('MODER')")
    public List<Approval> findAllWaitingForApprove() {
        return approvalRepository.findAll();
    }

    @PreAuthorize("hasRole('MODER')")
    public void approveOrReject(Integer approvalId, Boolean isApproved) throws ForumException {
        Optional<Approval> optionalApproval = approvalRepository.findById(approvalId);
        if (optionalApproval.isEmpty()) {
            throw new ForumObjectNotFoundException("No approval with id " + approvalId);
        }
        if (isApproved) {
            approvalRepository.deleteById(approvalId);
        } else {
            delete(optionalApproval.get().getTopic().getId());
        }
    }

    @PreAuthorize("hasRole('MODER')")
    public void approveByApprovalId(Integer approvalId) throws ForumObjectNotFoundException {
        Optional<Approval> optionalApproval = approvalRepository.findById(approvalId);
        if (optionalApproval.isEmpty()) {
            throw new ForumObjectNotFoundException("No approval with id " + approvalId);
        }
        approvalRepository.deleteById(approvalId);
    }

    public Pair<Topic, String> findTopicById(Integer topicId) throws ForumObjectNotFoundException, ForbiddenActionException {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Optional<Topic> optionalTopic = topicRepository.findById(topicId);
        if (optionalTopic.isEmpty()) {
            throw new ForumObjectNotFoundException("No topic with id " + topicId);
        }
        if (optionalTopic.get().getApproval() != null) {
            throw new ForbiddenActionException("Topic " + topicId + " is not approved by moderator.");
        }
        return Pair.of(optionalTopic.get(), userDetails == null ? "" : userDetails.getUsername());
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
    public Topic delete(Integer topicId) throws ForumException {
        TransactionExceptionKeeper keeper = new TransactionExceptionKeeper();
        Topic deletedTopic = transactionTemplate.execute(status -> {
            Pair<Path, Map<Path, Path>> backup = null;
            Topic topic;
            try {
                topic = topicRepository.findById(topicId)
                        .orElseThrow(() -> new NoSuchElementException("Topic " + topicId + " not found"));
            } catch (NoSuchElementException e) {
                keeper.setEx(new ForumObjectNotFoundException(e.getMessage()));
                return null;
            }
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
            return topic;
        });
        keeper.throwIfSet();
        return deletedTopic;
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
    public Approval add(Topic topic, Integer chapterId, @Nullable MultipartFile img1,
                                 @Nullable MultipartFile img2, @Nullable MultipartFile img3) throws ForumException {
        String login = ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .getUsername();
        TransactionExceptionKeeper keeper = new TransactionExceptionKeeper();
        Approval a = transactionTemplate.execute(status -> {
            Topic addedTopic;
            Approval approval;
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
                    approval = new Approval();
                    approval.setTopic(addedTopic);
                    approvalRepository.save(approval);
                } catch (Exception e) {
                    status.setRollbackOnly();
                    keeper.setEx(new ForumException("Error occurred when saving topic"));
                    return null;
                }
                List<String> imgLinks = new ArrayList<>();
                try {
                    if (img1 != null && !img1.isEmpty()) {
                        imgLinks.add(uploadImage(img1, 1, addedTopic.getId()).toString());
                    }
                    if (img2 != null && !img2.isEmpty()) {
                        imgLinks.add(uploadImage(img2, 2, addedTopic.getId()).toString());
                    }
                    if (img3 != null && !img3.isEmpty()) {
                        imgLinks.add(uploadImage(img3, 3, addedTopic.getId()).toString());
                    }
                } catch (ImageAccessException | IOException e) {
                    for (String imgLink : imgLinks) {
                        File imgFile = new File(imgLink);
                        if (!imgFile.delete()) {
                            System.err.println("Useless file " + imgLink + " was not deleted automatically!");
                        }
                    }
                    status.setRollbackOnly();
                    if (e instanceof ImageAccessException) {
                        keeper.setEx((ImageAccessException) e);
                    } else {
                        keeper.setEx(new ForumLogicException("Unable to save topic because we cannot upload the image",
                                HttpStatus.CONFLICT));
                    }
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
            return approval;
        });
        keeper.throwIfSet();
        return a;
    }

    private Path uploadImage(MultipartFile img, int i, int id) throws IOException, ImageAccessException {
        String imageFormat = detectImageFormat(img);
        String storageFilename = "t" + id + "_i" + i + "_image" + imageFormat;
        String storage = System.getenv("PHOTO_STORAGE");
        Path imagePath = Paths.get(storage).resolve(storageFilename);
        return Files.write(imagePath, img.getBytes());
    }

    private String detectImageFormat(MultipartFile perhapsImg) throws IOException, ImageAccessException {
        byte[] header = perhapsImg.getBytes();
        if (header.length > 8 &&
                header[0] == (byte) 0x89 && header[1] == (byte) 0x50 &&
                header[2] == (byte) 0x4E && header[3] == (byte) 0x47 &&
                header[4] == (byte) 0x0D && header[5] == (byte) 0x0A &&
                header[6] == (byte) 0x1A && header[7] == (byte) 0x0A) {
            return ".png";
        }
        if (header.length > 2 &&
                header[0] == (byte) 0xFF && header[1] == (byte) 0xD8) {
            return ".jpg";
        }
        throw new ImageAccessException("Wrong file format. Only png/jpg available", HttpStatus.BAD_REQUEST);
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
                    return optionalRating.get();
                } else {
                    rating.setCreator(client);
                    rating.setTopic(topic);
                    return ratingRepository.save(rating);
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
