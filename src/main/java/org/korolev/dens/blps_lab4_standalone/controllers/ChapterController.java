package org.korolev.dens.blps_lab4_standalone.controllers;

import org.korolev.dens.blps_lab4_standalone.entites.Chapter;
import org.korolev.dens.blps_lab4_standalone.exceptions.ForbiddenActionException;
import org.korolev.dens.blps_lab4_standalone.exceptions.ForumLogicException;
import org.korolev.dens.blps_lab4_standalone.exceptions.ForumObjectNotFoundException;
import org.korolev.dens.blps_lab4_standalone.services.ChapterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chapter")
public class ChapterController {

    private final ChapterService chapterService;

    public ChapterController(ChapterService chapterService) {
        this.chapterService = chapterService;
    }

    @PostMapping("/add")
    public ResponseEntity<?> addChapter(@RequestBody Chapter chapter) throws ForbiddenActionException {
        return ResponseEntity.ok(chapterService.add(chapter));
    }

    @DeleteMapping("/delete/{chapterId}")
    public ResponseEntity<?> deleteChapter(@PathVariable Integer chapterId) throws ForumLogicException,
            ForumObjectNotFoundException {
        chapterService.delete(chapterId);
        return ResponseEntity.ok("Chapter with id " + chapterId + " deleted successfully");
    }

    @GetMapping("/get/all")
    public List<Chapter> getAllChapters() {
        return chapterService.findAllChapters();
    }

}
