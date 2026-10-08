package com.gallery.service;

import com.gallery.dto.AlbumRequest;
import com.gallery.dto.AlbumResponse;
import com.gallery.exception.NotFoundException;
import com.gallery.mapper.AlbumMapper;
import com.gallery.model.Album;
import com.gallery.repository.AlbumRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlbumServiceTest {

    @Mock
    private AlbumRepository albumRepository;
    @Mock
    private AlbumMapper albumMapper;
    @InjectMocks
    private AlbumService albumService;

    private Album album(Long id, String owner) {
        Album a = new Album();
        a.setId(id);
        a.setName("Trip");
        a.setOwner(owner);
        return a;
    }

    private AlbumResponse response(Long id) {
        return new AlbumResponse(id, "Trip", "desc", null);
    }

    @Test
    void create_validRequest_savesAlbumWithOwnerAndReturnsResponse() {
        AlbumRequest request = new AlbumRequest("Trip", "desc");
        Album entity = new Album();
        AlbumResponse expected = response(1L);
        when(albumMapper.toEntity(request)).thenReturn(entity);
        when(albumRepository.save(entity)).thenReturn(entity);
        when(albumMapper.toResponse(entity)).thenReturn(expected);

        AlbumResponse result = albumService.create(request, "alice");

        assertEquals(expected, result);
        ArgumentCaptor<Album> captor = ArgumentCaptor.forClass(Album.class);
        verify(albumRepository).save(captor.capture());
        assertEquals("alice", captor.getValue().getOwner());
    }

    @Test
    void getAll_ownerHasAlbums_returnsMappedList() {
        Album a = album(1L, "alice");
        when(albumRepository.findByOwner("alice")).thenReturn(List.of(a));
        when(albumMapper.toResponse(a)).thenReturn(response(1L));

        List<AlbumResponse> result = albumService.getAll("alice");

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
    }

    @Test
    void getAll_ownerHasNoAlbums_returnsEmptyList() {
        when(albumRepository.findByOwner("bob")).thenReturn(List.of());

        List<AlbumResponse> result = albumService.getAll("bob");

        assertTrue(result.isEmpty());
        verifyNoInteractions(albumMapper);
    }

    @Test
    void getById_ownedAlbum_returnsResponse() {
        Album a = album(1L, "alice");
        when(albumRepository.findById(1L)).thenReturn(Optional.of(a));
        when(albumMapper.toResponse(a)).thenReturn(response(1L));

        AlbumResponse result = albumService.getById(1L, "alice");

        assertEquals(1L, result.id());
    }

    @Test
    void getById_albumOfAnotherOwner_throwsNotFound() {
        when(albumRepository.findById(1L)).thenReturn(Optional.of(album(1L, "alice")));

        assertThrows(NotFoundException.class, () -> albumService.getById(1L, "bob"));
    }

    @Test
    void getById_missingAlbum_throwsNotFound() {
        when(albumRepository.findById(99L)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> albumService.getById(99L, "alice"));
        assertTrue(ex.getMessage().contains("99"));
    }

    @Test
    void update_ownedAlbum_appliesChangesAndSaves() {
        Album a = album(1L, "alice");
        AlbumRequest request = new AlbumRequest("New", "new desc");
        when(albumRepository.findById(1L)).thenReturn(Optional.of(a));
        when(albumRepository.save(a)).thenReturn(a);
        when(albumMapper.toResponse(a)).thenReturn(response(1L));

        AlbumResponse result = albumService.update(1L, request, "alice");

        assertEquals(1L, result.id());
        verify(albumMapper).update(request, a);
        verify(albumRepository).save(a);
    }

    @Test
    void update_albumOfAnotherOwner_throwsNotFoundAndDoesNotSave() {
        when(albumRepository.findById(1L)).thenReturn(Optional.of(album(1L, "alice")));
        AlbumRequest request = new AlbumRequest("New", null);

        assertThrows(NotFoundException.class, () -> albumService.update(1L, request, "bob"));
        verify(albumRepository, never()).save(any());
    }

    @Test
    void delete_ownedAlbum_deletesEntity() {
        Album a = album(1L, "alice");
        when(albumRepository.findById(1L)).thenReturn(Optional.of(a));

        albumService.delete(1L, "alice");

        verify(albumRepository).delete(a);
    }

    @Test
    void delete_missingAlbum_throwsNotFoundAndNeverDeletes() {
        when(albumRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> albumService.delete(5L, "alice"));
        verify(albumRepository, never()).delete(any());
    }

    @Test
    void getOwnedEntity_ownedAlbum_returnsEntity() {
        Album a = album(1L, "alice");
        when(albumRepository.findById(1L)).thenReturn(Optional.of(a));

        Album result = albumService.getOwnedEntity(1L, "alice");

        assertSame(a, result);
    }

    @Test
    void getOwnedEntity_foreignAlbum_throwsNotFound() {
        when(albumRepository.findById(1L)).thenReturn(Optional.of(album(1L, "alice")));

        assertThrows(NotFoundException.class, () -> albumService.getOwnedEntity(1L, "bob"));
    }
}
