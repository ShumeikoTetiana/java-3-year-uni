package com.gallery.controller;

import com.gallery.dto.AlbumRequest;
import com.gallery.dto.AlbumResponse;
import com.gallery.exception.GlobalExceptionHandler;
import com.gallery.resolver.CurrentUserArgumentResolver;
import com.gallery.service.AlbumService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AlbumControllerTest {

    @Mock
    private AlbumService albumService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AlbumController(albumService))
                .setCustomArgumentResolvers(new CurrentUserArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private AlbumResponse response(Long id) {
        return new AlbumResponse(id, "Trip", "desc", null);
    }

    @Test
    void postCreated_metaAnnotationsResolveToPostMappingAndStatusCreated() throws Exception {
        Method create = AlbumController.class.getMethod("create", AlbumRequest.class, String.class);

        RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(create, RequestMapping.class);
        ResponseStatus responseStatus = AnnotatedElementUtils.findMergedAnnotation(create, ResponseStatus.class);

        assertNotNull(mapping);
        assertArrayEquals(new RequestMethod[]{RequestMethod.POST}, mapping.method());
        assertNotNull(responseStatus);
        assertEquals(HttpStatus.CREATED, responseStatus.code());
    }

    @Test
    void create_postRequest_returns201Created() throws Exception {
        when(albumService.create(any(AlbumRequest.class), eq("alice"))).thenReturn(response(1L));

        mockMvc.perform(post("/api/albums")
                        .principal(() -> "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Trip\",\"description\":\"desc\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void create_putOnSameUrl_returns405() throws Exception {
        mockMvc.perform(put("/api/albums")
                        .principal(() -> "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Trip\"}"))
                .andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(albumService);
    }

    @Test
    void create_invalidBody_returns400AndServiceIsNotCalled() throws Exception {
        mockMvc.perform(post("/api/albums")
                        .principal(() -> "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(albumService);
    }

    @Test
    void create_resolvedUsernameIsPassedToService() throws Exception {
        when(albumService.create(any(AlbumRequest.class), eq("alice"))).thenReturn(response(1L));

        mockMvc.perform(post("/api/albums")
                        .principal(() -> "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Trip\",\"description\":\"desc\"}"))
                .andExpect(status().isCreated());

        verify(albumService).create(new AlbumRequest("Trip", "desc"), "alice");
    }

    @Test
    void getAll_usernameDependsOnRequestPrincipal() throws Exception {
        when(albumService.getAll("bob")).thenReturn(List.of(response(7L)));

        String body = mockMvc.perform(get("/api/albums").principal(() -> "bob"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        verify(albumService).getAll("bob");
        assertTrue(body.contains("\"id\":7"));
    }
}
