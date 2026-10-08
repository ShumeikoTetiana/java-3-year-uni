package com.gallery.service;

import com.gallery.dto.PhotoRequest;
import com.gallery.dto.PhotoResponse;
import com.gallery.exception.NotFoundException;
import com.gallery.mapper.PhotoMapper;
import com.gallery.model.AccessLevel;
import com.gallery.model.Album;
import com.gallery.model.Photo;
import com.gallery.model.Tag;
import com.gallery.repository.PhotoRepository;
import com.gallery.repository.TagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhotoServiceTest {

    @Mock
    private PhotoRepository photoRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private AlbumService albumService;
    @Mock
    private PhotoMapper photoMapper;
    @InjectMocks
    private PhotoService photoService;

    private PhotoRequest request(Long albumId, Set<Long> tagIds) {
        return new PhotoRequest("Sunset", "http://img/1.jpg", "desc", AccessLevel.PRIVATE, albumId, tagIds);
    }

    private Photo photo(Long id, String owner) {
        Photo p = new Photo();
        p.setId(id);
        p.setOwner(owner);
        return p;
    }

    private PhotoResponse response(Long id) {
        return new PhotoResponse(id, "Sunset", "http://img/1.jpg", "desc", AccessLevel.PRIVATE, null, Set.of(), null);
    }

    @Test
    void create_withoutAlbumAndTags_savesPhotoWithOwner() {
        PhotoRequest request = request(null, null);
        Photo entity = new Photo();
        PhotoResponse expected = response(1L);
        when(photoMapper.toEntity(request)).thenReturn(entity);
        when(photoRepository.save(entity)).thenReturn(entity);
        when(photoMapper.toResponse(entity)).thenReturn(expected);

        PhotoResponse result = photoService.create(request, "alice");

        assertEquals(expected, result);
        ArgumentCaptor<Photo> captor = ArgumentCaptor.forClass(Photo.class);
        verify(photoRepository).save(captor.capture());
        assertEquals("alice", captor.getValue().getOwner());
        assertNull(captor.getValue().getAlbum());
        assertTrue(captor.getValue().getTags().isEmpty());
        verifyNoInteractions(albumService, tagRepository);
    }

    @Test
    void create_withAlbumAndTags_linksAlbumAndTags() {
        PhotoRequest request = request(5L, Set.of(1L));
        Photo entity = new Photo();
        Album album = new Album();
        Tag tag = new Tag();
        when(photoMapper.toEntity(request)).thenReturn(entity);
        when(albumService.getOwnedEntity(5L, "alice")).thenReturn(album);
        when(tagRepository.findAllById(Set.of(1L))).thenReturn(List.of(tag));
        when(photoRepository.save(entity)).thenReturn(entity);
        when(photoMapper.toResponse(entity)).thenReturn(response(1L));

        photoService.create(request, "alice");

        ArgumentCaptor<Photo> captor = ArgumentCaptor.forClass(Photo.class);
        verify(photoRepository).save(captor.capture());
        assertSame(album, captor.getValue().getAlbum());
        assertTrue(captor.getValue().getTags().contains(tag));
    }

    @Test
    void create_foreignAlbum_throwsNotFoundAndDoesNotSave() {
        PhotoRequest request = request(5L, null);
        when(photoMapper.toEntity(request)).thenReturn(new Photo());
        when(albumService.getOwnedEntity(5L, "alice")).thenThrow(new NotFoundException("Album not found: 5"));

        assertThrows(NotFoundException.class, () -> photoService.create(request, "alice"));
        verify(photoRepository, never()).save(any());
    }

    @Test
    void create_unknownTagId_throwsNotFoundAndDoesNotSave() {
        PhotoRequest request = request(null, Set.of(9L));
        when(photoMapper.toEntity(request)).thenReturn(new Photo());
        when(tagRepository.findAllById(Set.of(9L))).thenReturn(List.of());

        assertThrows(NotFoundException.class, () -> photoService.create(request, "alice"));
        verify(photoRepository, never()).save(any());
    }

    @Test
    void getAll_ownerHasPhotos_returnsMappedList() {
        Photo p = photo(1L, "alice");
        when(photoRepository.findByOwner("alice")).thenReturn(List.of(p));
        when(photoMapper.toResponse(p)).thenReturn(response(1L));

        List<PhotoResponse> result = photoService.getAll("alice");

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
    }

    @Test
    void getAll_ownerHasNoPhotos_returnsEmptyList() {
        when(photoRepository.findByOwner("bob")).thenReturn(List.of());

        List<PhotoResponse> result = photoService.getAll("bob");

        assertTrue(result.isEmpty());
    }

    @Test
    void getById_ownedPhoto_returnsResponse() {
        Photo p = photo(1L, "alice");
        when(photoRepository.findById(1L)).thenReturn(Optional.of(p));
        when(photoMapper.toResponse(p)).thenReturn(response(1L));

        PhotoResponse result = photoService.getById(1L, "alice");

        assertEquals(1L, result.id());
    }

    @Test
    void getById_photoOfAnotherOwner_throwsNotFound() {
        when(photoRepository.findById(1L)).thenReturn(Optional.of(photo(1L, "alice")));

        assertThrows(NotFoundException.class, () -> photoService.getById(1L, "bob"));
    }

    @Test
    void getById_missingPhoto_throwsNotFound() {
        when(photoRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> photoService.getById(9L, "alice"));
    }

    @Test
    void update_ownedPhoto_appliesChangesAndSaves() {
        Photo p = photo(1L, "alice");
        PhotoRequest request = request(null, null);
        when(photoRepository.findById(1L)).thenReturn(Optional.of(p));
        when(photoRepository.save(p)).thenReturn(p);
        when(photoMapper.toResponse(p)).thenReturn(response(1L));

        PhotoResponse result = photoService.update(1L, request, "alice");

        assertEquals(1L, result.id());
        verify(photoMapper).update(request, p);
        verify(photoRepository).save(p);
    }

    @Test
    void update_photoOfAnotherOwner_throwsNotFoundAndDoesNotSave() {
        when(photoRepository.findById(1L)).thenReturn(Optional.of(photo(1L, "alice")));
        PhotoRequest request = request(null, null);

        assertThrows(NotFoundException.class, () -> photoService.update(1L, request, "bob"));
        verify(photoRepository, never()).save(any());
    }

    @Test
    void delete_ownedPhoto_deletesEntity() {
        Photo p = photo(1L, "alice");
        when(photoRepository.findById(1L)).thenReturn(Optional.of(p));

        photoService.delete(1L, "alice");

        verify(photoRepository).delete(p);
    }

    @Test
    void delete_missingPhoto_throwsNotFoundAndNeverDeletes() {
        when(photoRepository.findById(3L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> photoService.delete(3L, "alice"));
        verify(photoRepository, never()).delete(any());
    }

    @Test
    void getAllForAdmin_photosExist_returnsAllMapped() {
        Photo p1 = photo(1L, "alice");
        Photo p2 = photo(2L, "bob");
        when(photoRepository.findAll()).thenReturn(List.of(p1, p2));
        when(photoMapper.toResponse(p1)).thenReturn(response(1L));
        when(photoMapper.toResponse(p2)).thenReturn(response(2L));

        List<PhotoResponse> result = photoService.getAllForAdmin();

        assertEquals(2, result.size());
    }

    @Test
    void getAllForAdmin_noPhotos_returnsEmptyList() {
        when(photoRepository.findAll()).thenReturn(List.of());

        List<PhotoResponse> result = photoService.getAllForAdmin();

        assertTrue(result.isEmpty());
    }
}
