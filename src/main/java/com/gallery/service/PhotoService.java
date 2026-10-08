package com.gallery.service;

import com.gallery.dto.PhotoRequest;
import com.gallery.dto.PhotoResponse;
import com.gallery.exception.NotFoundException;
import com.gallery.mapper.PhotoMapper;
import com.gallery.model.Photo;
import com.gallery.model.Tag;
import com.gallery.repository.PhotoRepository;
import com.gallery.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class PhotoService {

    private final PhotoRepository photoRepository;
    private final TagRepository tagRepository;
    private final AlbumService albumService;
    private final PhotoMapper photoMapper;

    public PhotoResponse create(PhotoRequest request, String owner) {
        Photo photo = photoMapper.toEntity(request);
        photo.setOwner(owner);
        linkAlbumAndTags(photo, request, owner);
        return photoMapper.toResponse(photoRepository.save(photo));
    }

    @Transactional(readOnly = true)
    public List<PhotoResponse> getAll(String owner) {
        return photoRepository.findByOwner(owner).stream().map(photoMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PhotoResponse getById(Long id, String owner) {
        return photoMapper.toResponse(getOwnedEntity(id, owner));
    }

    public PhotoResponse update(Long id, PhotoRequest request, String owner) {
        Photo photo = getOwnedEntity(id, owner);
        photoMapper.update(request, photo);
        linkAlbumAndTags(photo, request, owner);
        return photoMapper.toResponse(photoRepository.save(photo));
    }

    public void delete(Long id, String owner) {
        photoRepository.delete(getOwnedEntity(id, owner));
    }

    /** Доступ лише для ADMIN — перевіряється через @PreAuthoriz. */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<PhotoResponse> getAllForAdmin() {
        return photoRepository.findAll().stream().map(photoMapper::toResponse).toList();
    }

    private Photo getOwnedEntity(Long id, String owner) {
        return photoRepository.findById(id)
                .filter(photo -> photo.getOwner().equals(owner))
                .orElseThrow(() -> new NotFoundException("Photo not found: " + id));
    }

    private void linkAlbumAndTags(Photo photo, PhotoRequest request, String owner) {
        photo.setAlbum(request.albumId() == null ? null : albumService.getOwnedEntity(request.albumId(), owner));
        photo.setTags(resolveTags(request.tagIds()));
    }

    private Set<Tag> resolveTags(Set<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return new HashSet<>();
        }
        List<Tag> found = tagRepository.findAllById(tagIds);
        if (found.size() != tagIds.size()) {
            throw new NotFoundException("Some tags were not found: " + tagIds);
        }
        return new HashSet<>(found);
    }
}
