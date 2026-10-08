package com.gallery.service;

import com.gallery.dto.AlbumRequest;
import com.gallery.dto.AlbumResponse;
import com.gallery.exception.NotFoundException;
import com.gallery.mapper.AlbumMapper;
import com.gallery.model.Album;
import com.gallery.repository.AlbumRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AlbumService {

    private final AlbumRepository albumRepository;
    private final AlbumMapper albumMapper;

    public AlbumResponse create(AlbumRequest request, String owner) {
        Album album = albumMapper.toEntity(request);
        album.setOwner(owner);
        return albumMapper.toResponse(albumRepository.save(album));
    }

    @Transactional(readOnly = true)
    public List<AlbumResponse> getAll(String owner) {
        return albumRepository.findByOwner(owner).stream().map(albumMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AlbumResponse getById(Long id, String owner) {
        return albumMapper.toResponse(getOwnedEntity(id, owner));
    }

    public AlbumResponse update(Long id, AlbumRequest request, String owner) {
        Album album = getOwnedEntity(id, owner);
        albumMapper.update(request, album);
        return albumMapper.toResponse(albumRepository.save(album));
    }

    public void delete(Long id, String owner) {
        albumRepository.delete(getOwnedEntity(id, owner));
    }

    /** Повертає альбом, лише якщо він належить користувачу (ізоляція приватного простору). */
    public Album getOwnedEntity(Long id, String owner) {
        return albumRepository.findById(id)
                .filter(album -> album.getOwner().equals(owner))
                .orElseThrow(() -> new NotFoundException("Album not found: " + id));
    }
}
