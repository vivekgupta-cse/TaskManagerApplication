package com.taskmanager.app.mapper;

import com.taskmanager.app.dto.TaskResponseDTO;
import com.taskmanager.app.model.Task;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")  // MapStruct Makes this a Spring Bean (injectable)
public interface TaskMapper {

    // Entity → DTO
    @Mapping(source = "header", target = "title")  // header → title (field rename)
    @Mapping(
            target = "completionStatus",
            expression = "java(task.isCompleted() ? \"DONE\" : \"PENDING\")"  // computed!
    )
    TaskResponseDTO toDTO(Task task);

    // DTO → Entity (inverse of the above, auto-derived by MapStruct)
    @InheritInverseConfiguration
    // Reverses all mappings: title → header
    //@Mapping(source = "title", target = "header")
    Task toEntity(TaskResponseDTO dto);
}