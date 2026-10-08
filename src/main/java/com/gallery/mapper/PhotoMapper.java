package com.gallery.mapper;

import com.gallery.dto.PhotoRequest;
import com.gallery.dto.PhotoResponse;
import com.gallery.model.Photo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = TagMapper.class)
public interface PhotoMapper {
    @Mapping(source = "album.id", target = "albumId")
    PhotoResponse toResponse(Photo photo);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "album", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Photo toEntity(PhotoRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "album", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void update(PhotoRequest request, @MappingTarget Photo photo);
}
