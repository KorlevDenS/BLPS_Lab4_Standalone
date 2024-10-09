package org.korolev.dens.blps_lab4_standalone.services;

import org.korolev.dens.blps_lab4_standalone.entites.Chapter;
import org.korolev.dens.blps_lab4_standalone.entites.Client;
import org.korolev.dens.blps_lab4_standalone.exceptions.ForbiddenActionException;
import org.korolev.dens.blps_lab4_standalone.exceptions.ForumLogicException;
import org.korolev.dens.blps_lab4_standalone.exceptions.ForumObjectNotFoundException;
import org.korolev.dens.blps_lab4_standalone.repositories.ChapterRepository;
import org.korolev.dens.blps_lab4_standalone.repositories.ClientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ChapterService {

    private final ClientRepository clientRepository;
    private final ChapterRepository chapterRepository;

    public ChapterService(ClientRepository clientRepository, ChapterRepository chapterRepository) {
        this.clientRepository = clientRepository;
        this.chapterRepository = chapterRepository;
    }

    public List<Chapter> findAllChapters() {
        return chapterRepository.findAll();
    }

    @PreAuthorize("hasRole('MODER')")
    public Chapter add(Chapter chapter) throws ForbiddenActionException {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Optional<Client> optionalClient = clientRepository.findByLogin(userDetails.getUsername());
        if (optionalClient.isEmpty()) {
            throw new ForbiddenActionException("Illegal access to resource: no such client in database");
        }
        chapter.setCreator(optionalClient.get());
        return chapterRepository.save(chapter);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void delete(Integer chapterId) throws ForumObjectNotFoundException, ForumLogicException {
        Optional<Chapter> chapter = chapterRepository.findById(chapterId);
        if (chapter.isEmpty()) {
            throw new ForumObjectNotFoundException("Chapter with id " + chapterId + " not found");
        }
        if (!chapter.get().getTopics().isEmpty()) {
            throw new ForumLogicException("Chapter is not empty! Firstly delete all topics in it.",
                    HttpStatus.PRECONDITION_REQUIRED);
        }
        chapterRepository.deleteById(chapterId);
    }

}
