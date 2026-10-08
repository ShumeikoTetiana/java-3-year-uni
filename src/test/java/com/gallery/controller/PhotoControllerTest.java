package com.gallery.controller;

import com.gallery.dto.PhotoRequest;
import com.gallery.dto.PhotoResponse;
import com.gallery.exception.GlobalExceptionHandler;
import com.gallery.model.AccessLevel;
import com.gallery.resolver.CurrentUserArgumentResolver;
import com.gallery.service.PhotoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PhotoControllerTest {

    @Mock
    private PhotoService photoService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PhotoController(photoService))
                .setCustomArgumentResolvers(new CurrentUserArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private PhotoResponse response(Long id) {
        return new PhotoResponse(id, "Sunset", "https://example.com/a.jpg", "desc",
                AccessLevel.PUBLIC, null, Set.of(), null);
    }

    private String json(String accessLevel, String url, String tagIds) {
        return "{\"title\":\"Sunset\",\"url\":\"" + url + "\",\"description\":\"desc\","
                + "\"accessLevel\":\"" + accessLevel + "\",\"albumId\":null,\"tagIds\":" + tagIds + "}";
    }

    @Test
    void create_validPublicPhoto_returns201AndUsesCurrentUser() throws Exception {
        when(photoService.create(any(PhotoRequest.class), eq("alice"))).thenReturn(response(5L));

        mockMvc.perform(post("/api/photos")
                        .principal(() -> "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("PUBLIC", "https://example.com/a.jpg", "[1]")))
                .andExpect(status().isCreated());

        verify(photoService).create(
                new PhotoRequest("Sunset", "https://example.com/a.jpg", "desc", AccessLevel.PUBLIC, null, Set.of(1L)),
                "alice");
    }

    @Test
    void create_publicPhotoWithoutTags_returns400WithClearMessage() throws Exception {
        String body = mockMvc.perform(post("/api/photos")
                        .principal(() -> "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("PUBLIC", "https://example.com/a.jpg", "[]")))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertTrue(body.contains("at least one tag"), body);
        verifyNoInteractions(photoService);
    }

    @Test
    void create_publicPhotoWithHttpUrl_returns400WithClearMessage() throws Exception {
        String body = mockMvc.perform(post("/api/photos")
                        .principal(() -> "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("PUBLIC", "http://example.com/a.jpg", "[1]")))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertTrue(body.contains("https://"), body);
        verifyNoInteractions(photoService);
    }

    @Test
    void create_privatePhotoWithoutTags_returns201() throws Exception {
        when(photoService.create(any(PhotoRequest.class), eq("alice"))).thenReturn(response(6L));

        mockMvc.perform(post("/api/photos")
                        .principal(() -> "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("PRIVATE", "http://example.com/a.jpg", "[]")))
                .andExpect(status().isCreated());
    }

    @Test
    void getAll_usernameFromRequestIsUsedToLoadPhotos() throws Exception {
        when(photoService.getAll("bob")).thenReturn(List.of(response(9L)));

        String body = mockMvc.perform(get("/api/photos").principal(() -> "bob"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        verify(photoService).getAll("bob");
        assertTrue(body.contains("\"id\":9"));
    }
}
