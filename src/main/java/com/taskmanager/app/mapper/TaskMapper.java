package com.taskmanager.app.mapper;

import com.taskmanager.app.dto.TaskRequestDTO;
import com.taskmanager.app.dto.TaskResponseDTO;
import com.taskmanager.app.model.Task;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")  // MapStruct Makes this a Spring Bean (injectable)
public interface TaskMapper {

    // Entity → ResponseDTO (for ALL reads: GET, and after POST/PUT)
    @Mapping(source = "header", target = "title")  // header (Java field) → title (DTO/DB column)
    @Mapping(
            target = "completionStatus",
            expression = "java(task.isCompleted() ? \"DONE\" : \"PENDING\")"  // server-computed field
    )
    TaskResponseDTO toDTO(Task task);

    // RequestDTO → Entity (for CREATE and UPDATE — client sends title, maps to header)
    @Mapping(source = "title", target = "header")  // title (client input) → header (Java field)
    @Mapping(target = "id", ignore = true)          // id is always DB-generated, never from client
    Task toEntity(TaskRequestDTO dto);
}