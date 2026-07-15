package com.riddhic.aiengineering.service;

import com.riddhic.aiengineering.dto.TaskResponse;
import com.riddhic.aiengineering.model.Task;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TaskService {

    private final List<Task> taskList = new ArrayList<>();


    public Task getTaskbyId(String taskId) {
        for (Task t : taskList) {
            if (t.getId().equals(taskId)) {
                return t;
            }
        }
        throw new IllegalArgumentException(
                "No task found with ID " + taskId
        );
    }

    public List<Task> getallTasks() {
        if (taskList.isEmpty()) {
            throw new IllegalArgumentException("No tasks available");
        }
        return taskList;
    }

    public TaskResponse createTask(Task task) {
        taskList.add(task);
        return new TaskResponse(task);
    }

    public TaskResponse updateTask(String id, Task task) {
        Task existingTask = getTaskbyId(id);
        existingTask.setTitle(task.getTitle());
        existingTask.setStatus(task.getStatus());
        existingTask.setDescription(task.getDescription());
        existingTask.setPriority(task.getPriority());
        return task;
    }

    public Task deleteTask(String id) {
        Task task = getTaskbyId(id);
        taskList.remove(task);
        return task;
    }
}
