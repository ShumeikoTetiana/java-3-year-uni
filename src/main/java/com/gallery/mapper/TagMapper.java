package com.gallery.mapper;

import com.gallery.dto.TagDto;
import com.gallery.model.Tag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TagMapper {
    TagDto toDto(Tag tag);

    @Mapping(target = "id", ignore = true)
    Tag toEntity(TagDto dto);

    @Mapping(target = "id", ignore = true)
    void update(TagDto dto, @MappingTarget Tag tag);
}
