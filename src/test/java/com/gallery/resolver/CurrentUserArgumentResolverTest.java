package com.gallery.resolver;

import com.gallery.annotation.CurrentUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.context.request.NativeWebRequest;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserArgumentResolverTest {

    static class Handlers {
        void withAnnotation(@CurrentUser String username) {}

        void withoutAnnotation(String username) {}

        void withAnnotationWrongType(@CurrentUser Long id) {}
    }

    @Mock
    private NativeWebRequest request;

    private final CurrentUserArgumentResolver resolver = new CurrentUserArgumentResolver();

    private MethodParameter parameter(String method, Class<?> type) throws NoSuchMethodException {
        Method m = Handlers.class.getDeclaredMethod(method, type);
        return new MethodParameter(m, 0);
    }

    @Test
    void supportsParameter_stringWithCurrentUser_returnsTrue() throws Exception {
        MethodParameter parameter = parameter("withAnnotation", String.class);

        assertTrue(resolver.supportsParameter(parameter));
    }

    @Test
    void supportsParameter_stringWithoutAnnotation_returnsFalse() throws Exception {
        MethodParameter parameter = parameter("withoutAnnotation", String.class);

        assertFalse(resolver.supportsParameter(parameter));
    }

    @Test
    void supportsParameter_currentUserOnNonStringParameter_returnsFalse() throws Exception {
        MethodParameter parameter = parameter("withAnnotationWrongType", Long.class);

        assertFalse(resolver.supportsParameter(parameter));
    }

    @Test
    void resolveArgument_authenticatedRequest_returnsUsernameAsString() throws Exception {
        when(request.getUserPrincipal()).thenReturn(() -> "alice");
        MethodParameter parameter = parameter("withAnnotation", String.class);

        Object result = resolver.resolveArgument(parameter, null, request, null);

        assertInstanceOf(String.class, result);
        assertEquals("alice", result);
    }

    @Test
    void resolveArgument_anonymousRequest_throwsAuthenticationException() throws Exception {
        when(request.getUserPrincipal()).thenReturn(null);
        MethodParameter parameter = parameter("withAnnotation", String.class);

        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> resolver.resolveArgument(parameter, null, request, null));
    }
}
