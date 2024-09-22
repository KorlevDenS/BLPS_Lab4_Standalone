package org.korolev.dens.blps_lab4_standalone.services;

import jakarta.annotation.Nullable;
import org.korolev.dens.blps_lab4_standalone.entites.*;
import org.korolev.dens.blps_lab4_standalone.exceptions.ImageAccessException;
import org.korolev.dens.blps_lab4_standalone.exceptions.ImagesBackupException;
import org.korolev.dens.blps_lab4_standalone.repositories.*;
import org.korolev.dens.blps_lab4_standalone.requests.StatsMessage;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
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

    public ResponseEntity<?> findTopicById(Integer topicId, UserDetails userDetails) {
        Optional<Topic> optionalTopic = topicRepository.findById(topicId);
        if (optionalTopic.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No topic with id " + topicId);
        }
        if (userDetails == null) {
            messageProducer.sendMessage(new StatsMessage(optionalTopic.get().getId(), "", "watch",
                    optionalTopic.get().getOwner().getLogin()));
            System.out.println("WATCH WITHOUT PRODUCER");
        } else {
            messageProducer.sendMessage(new StatsMessage(optionalTopic.get().getId(), userDetails.getUsername(),
                    "watch", optionalTopic.get().getOwner().getLogin()));
            System.out.println("WATCH WITH PRODUCER");
        }
        return ResponseEntity.ok(optionalTopic.get());
    }

    public ResponseEntity<?> findImageByURL(String URL) {
        Resource resource;
        try {
            resource = getImageFromDisk(URL);
        } catch (ImageAccessException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No image with URL " + URL);
        }
        return ResponseEntity.ok(resource);
    }

    private Resource getImageFromDisk(String imageUrl) throws ImageAccessException {
        try {
            String storage = System.getenv("PHOTO_STORAGE");
            Path filepath = Paths.get(storage).resolve(imageUrl);
            Resource resource = new UrlResource(filepath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new ImageAccessException("Image does not exist");
            }
        } catch (Exception e) {
            throw new ImageAccessException("Cased by " + e.getClass() + ": " + e.getMessage());
        }
    }

    public ResponseEntity<?> delete(Integer topicId) {
        return transactionTemplate.execute((TransactionCallback<ResponseEntity<?>>) status -> {
            Pair<Path, Map<Path, Path>> backup = null;
            Topic topic;
            try {
                topic = topicRepository.findById(topicId)
                        .orElseThrow(() -> new NoSuchElementException("Topic not found"));
            } catch (NoSuchElementException e) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
            }
            Client topicOwner = topic.getOwner();
            try {
                try {
                    backup = makeImgBackup(topic);
                } catch (ImagesBackupException e) {
                    status.setRollbackOnly();
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
                }
                try {
                    for (Image image : topic.getImages()) {
                        imageRepository.deleteById(image.getId());
                    }
                } catch (Exception e) {
                    status.setRollbackOnly();
                    deleteReservedFiles(backup.getFirst());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not delete images from db");
                }
                try {
                    topicRepository.deleteById(topicId);
                } catch (Exception e) {
                    status.setRollbackOnly();
                    deleteReservedFiles(backup.getFirst());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not delete topic from db");
                }
                for (Image image : topic.getImages()) {
                    String imgLink = image.getLink();
                    File file = new File(imgLink);
                    if (!file.delete()) {
                        restoreDeletedFiles(backup.getSecond());
                        status.setRollbackOnly();
                        deleteReservedFiles(backup.getFirst());
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("Could not delete image from disk");
                    }
                }
            } catch (Exception e) {
                status.setRollbackOnly();
                if (backup != null) deleteReservedFiles(backup.getFirst());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Topic with id " + topicId + " was not deleted");
            }
            deleteReservedFiles(backup.getFirst());
            messageProducer.sendMessage(new StatsMessage(topicId, "", "delete",
                    topicOwner.getLogin()));
            return ResponseEntity.status(HttpStatus.OK).body("Topic with id " + topicId + " deleted");
        });
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

    public ResponseEntity<?> add(Topic topic, Integer chapterId, String login, @Nullable MultipartFile img1,
                                 @Nullable MultipartFile img2, @Nullable MultipartFile img3) {
        MultipartFile[] multipartFiles = {img1, img2, img3};
        if (!validateImgList(Arrays.asList(multipartFiles))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Неверный формат файла. Допустимы png и jpg");
        }
        return transactionTemplate.execute(status -> {
            Topic addedTopic;
            try {
                Optional<Chapter> optionalChapter = chapterRepository.findById(chapterId);
                Optional<Client> optionalClient = clientRepository.findByLogin(login);
                if (optionalClient.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Пользователь не авторизован");
                } else if (optionalChapter.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No chapter with id " + chapterId);
                }
                topic.setOwner(optionalClient.get());
                topic.setChapter(optionalChapter.get());
                try {
                    addedTopic = topicRepository.save(topic);
                } catch (Exception e) {
                    status.setRollbackOnly();
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error occurred when saving topic");
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
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body("Unable to save topic because we cannot upload the image");
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
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("Error occurred when saving image");
                    }
                }
            } catch (Exception e) {
                status.setRollbackOnly();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Topic was not added");
            }
            messageProducer.sendMessage(new StatsMessage(addedTopic.getId(), "", "add", login));
            return ResponseEntity.ok(addedTopic);
        });
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

    public ResponseEntity<?> subscribe(Integer topicId, String login) {
        Optional<Client> optionalClient = clientRepository.findByLogin(login);
        Optional<Topic> optionalTopic = topicRepository.findById(topicId);
        if (optionalClient.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Пользователь не авторизован");
        } else if (optionalTopic.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Тема для подписки не существует");
        }
        Optional<Subscription> optionalSubscription =
                subscriptionRepository.findByClientAndTopic(optionalClient.get(), optionalTopic.get());
        if (optionalSubscription.isPresent()) {
            return ResponseEntity.status(HttpStatus.ALREADY_REPORTED).body("Подписка на эту тему уже оформлена");
        } else {
            Subscription subscription = new Subscription();
            SubscriptionId subscriptionId = new SubscriptionId();
            subscriptionId.setClient(optionalClient.get().getId());
            subscriptionId.setTopic(topicId);
            subscription.setId(subscriptionId);
            subscription.setClient(optionalClient.get());
            subscription.setTopic(optionalTopic.get());
            Subscription addedSubscription = subscriptionRepository.save(subscription);
            return ResponseEntity.ok(addedSubscription);
        }
    }

    public ResponseEntity<?> unsubscribe(Integer topicId, String login) {
        return transactionTemplate.execute((TransactionCallback<ResponseEntity<?>>) status -> {
            try {
                Client client = clientRepository.findByLogin(login).orElseThrow(Exception::new);
                Topic topic = topicRepository.findById(topicId)
                        .orElseThrow(() -> new NoSuchElementException("Topic not found"));
                subscriptionRepository.findByClientAndTopic(client, topic)
                        .orElseThrow(() -> new NoSuchElementException("Subscription not found"));
                subscriptionRepository.deleteByClientAndTopic(client, topic);
            } catch (NoSuchElementException e) {
                status.setRollbackOnly();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
            } catch (Exception e) {
                status.setRollbackOnly();
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Не удалось отписаться");
            }
            return ResponseEntity.status(HttpStatus.OK).body("Подписка успешно удалена");

        });
    }

    public ResponseEntity<?> update(Topic topic, String login) {
        return transactionTemplate.execute((TransactionCallback<ResponseEntity<?>>) status -> {
            try {
                Topic t = topicRepository.findById(topic.getId())
                        .orElseThrow(() -> new NoSuchElementException("Topic not found"));
                if (!(t.getOwner().getLogin().equals(login))) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body("Topic with id " + topic.getId() + " is not yours");
                }
                if (!Objects.equals(t.getTitle(), topic.getTitle())) {
                    topicRepository.updateTitle(t.getId(), topic.getTitle(), login);
                }
                if (!Objects.equals(t.getText(), topic.getText())) {
                    topicRepository.updateText(t.getId(), topic.getText(), login);
                }
            } catch (NoSuchElementException e) {
                status.setRollbackOnly();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
            } catch (Exception e) {
                status.setRollbackOnly();
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Update failed");
            }
            return ResponseEntity.status(HttpStatus.OK).body("Topic " + topic.getId() + " has been updated");
        });
    }

    public ResponseEntity<?> rate(Rating rating, Integer topicId, String login) {
        return transactionTemplate.execute(status -> {
            try {
                Client client = clientRepository.findByLogin(login).orElseThrow(Exception::new);
                Topic topic = topicRepository.findById(topicId)
                        .orElseThrow(() -> new NoSuchElementException("Topic not found"));
                Optional<Rating> optionalRating = ratingRepository.findRatingByCreatorAndTopic(client, topic);
                if (optionalRating.isPresent()) {
                    ratingRepository.updateRatingByClientAndTopic(login, rating.getRating(), topicId);
                    messageProducer.sendMessage(new StatsMessage(topicId, login, rating.getRating().toString(),
                            topic.getOwner().getLogin()));
                    return ResponseEntity.status(HttpStatus.OK).body("Оценка успешно обновлена");
                } else {
                    rating.setCreator(client);
                    rating.setTopic(topic);
                    Rating addedRating = ratingRepository.save(rating);
                    messageProducer.sendMessage(new StatsMessage(topicId, login, rating.getRating().toString(),
                            topic.getOwner().getLogin()));
                    return ResponseEntity.ok(addedRating);
                }
            } catch (NoSuchElementException e) {
                status.setRollbackOnly();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
            } catch (Exception e) {
                status.setRollbackOnly();
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Не удалось оценить тему");
            }
        });
    }

}
