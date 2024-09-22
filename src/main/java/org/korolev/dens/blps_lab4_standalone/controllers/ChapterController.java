package org.korolev.dens.blps_lab4_standalone.controllers;

import org.korolev.dens.blps_lab4_standalone.entites.Chapter;
import org.korolev.dens.blps_lab4_standalone.entites.Client;
import org.korolev.dens.blps_lab4_standalone.repositories.ChapterRepository;
import org.korolev.dens.blps_lab4_standalone.repositories.ClientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/chapter")
public class ChapterController {

    private final ClientRepository clientRepository;
    private final ChapterRepository chapterRepository;

    public ChapterController(ClientRepository clientRepository,
                             ChapterRepository chapterRepository) {
        this.clientRepository = clientRepository;
        this.chapterRepository = chapterRepository;
    }

    @PreAuthorize("hasRole('MODER')")
    @PostMapping("/add")
    public ResponseEntity<?> addChapter(@RequestBody Chapter chapter,
                                        @AuthenticationPrincipal UserDetails userDetails) {
        Optional<Client> optionalClient = clientRepository.findByLogin(userDetails.getUsername());
        if (optionalClient.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Пользователь не авторизован");
        }
        chapter.setCreator(optionalClient.get());
        Chapter addedChapter = chapterRepository.save(chapter);
        return ResponseEntity.ok(addedChapter);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/delete/{chapterId}")
    public ResponseEntity<?> deleteChapter(@PathVariable Integer chapterId) {
        Optional<Chapter> chapter = chapterRepository.findById(chapterId);
        if (chapter.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Chapter with id " + chapterId + " not found");
        }
        if (!chapter.get().getTopics().isEmpty()) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED)
                    .body("Chapter is not empty! Firstly delete all topics in it.");
        }
        chapterRepository.deleteById(chapterId);
        return ResponseEntity.ok("Chapter with id " + chapterId + " deleted successfully");
    }

    @GetMapping("/get/all")
    public List<Chapter> getAllChapters() {
        return chapterRepository.findAll();
    }

}
