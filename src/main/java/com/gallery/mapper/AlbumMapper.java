package com.gallery.mapper;

import com.gallery.dto.AlbumRequest;
import com.gallery.dto.AlbumResponse;
import com.gallery.model.Album;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AlbumMapper {
    AlbumResponse toResponse(Album album);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Album toEntity(AlbumRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void update(AlbumRequest request, @MappingTarget Album album);
}
