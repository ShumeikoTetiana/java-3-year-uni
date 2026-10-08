package com.gallery.service;

import com.gallery.dto.TagDto;
import com.gallery.exception.NotFoundException;
import com.gallery.mapper.TagMapper;
import com.gallery.model.Tag;
import com.gallery.repository.TagRepository;
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
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;
    @Mock
    private TagMapper tagMapper;
    @InjectMocks
    private TagService tagService;

    private Tag tag(Long id, String name) {
        Tag t = new Tag();
        t.setId(id);
        t.setName(name);
        return t;
    }

    @Test
    void create_newName_savesTagAndReturnsDto() {
        TagDto request = new TagDto(null, "nature");
        Tag entity = tag(null, "nature");
        Tag saved = tag(1L, "nature");
        TagDto expected = new TagDto(1L, "nature");
        when(tagRepository.existsByName("nature")).thenReturn(false);
        when(tagMapper.toEntity(request)).thenReturn(entity);
        when(tagRepository.save(entity)).thenReturn(saved);
        when(tagMapper.toDto(saved)).thenReturn(expected);

        TagDto result = tagService.create(request);

        assertEquals(expected, result);
        ArgumentCaptor<Tag> captor = ArgumentCaptor.forClass(Tag.class);
        verify(tagRepository).save(captor.capture());
        assertEquals("nature", captor.getValue().getName());
    }

    @Test
    void create_duplicateName_throwsIllegalArgumentAndDoesNotSave() {
        when(tagRepository.existsByName("nature")).thenReturn(true);
        TagDto request = new TagDto(null, "nature");

        assertThrows(IllegalArgumentException.class, () -> tagService.create(request));
        verify(tagRepository, never()).save(any());
    }

    @Test
    void getAll_tagsExist_returnsMappedList() {
        Tag t = tag(1L, "nature");
        when(tagRepository.findAll()).thenReturn(List.of(t));
        when(tagMapper.toDto(t)).thenReturn(new TagDto(1L, "nature"));

        List<TagDto> result = tagService.getAll();

        assertEquals(1, result.size());
        assertEquals("nature", result.get(0).name());
    }

    @Test
    void getAll_noTags_returnsEmptyList() {
        when(tagRepository.findAll()).thenReturn(List.of());

        List<TagDto> result = tagService.getAll();

        assertTrue(result.isEmpty());
    }

    @Test
    void update_existingTag_appliesChangesAndSaves() {
        Tag t = tag(1L, "old");
        TagDto request = new TagDto(null, "new");
        when(tagRepository.findById(1L)).thenReturn(Optional.of(t));
        when(tagRepository.save(t)).thenReturn(t);
        when(tagMapper.toDto(t)).thenReturn(new TagDto(1L, "new"));

        TagDto result = tagService.update(1L, request);

        assertEquals("new", result.name());
        verify(tagMapper).update(request, t);
        verify(tagRepository).save(t);
    }

    @Test
    void update_missingTag_throwsNotFoundAndDoesNotSave() {
        when(tagRepository.findById(7L)).thenReturn(Optional.empty());
        TagDto request = new TagDto(null, "x");

        assertThrows(NotFoundException.class, () -> tagService.update(7L, request));
        verify(tagRepository, never()).save(any());
    }

    @Test
    void delete_existingTag_deletesById() {
        when(tagRepository.existsById(1L)).thenReturn(true);

        tagService.delete(1L);

        verify(tagRepository).deleteById(1L);
    }

    @Test
    void delete_missingTag_throwsNotFoundAndNeverDeletes() {
        when(tagRepository.existsById(7L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> tagService.delete(7L));
        verify(tagRepository, never()).deleteById(any());
    }
}
