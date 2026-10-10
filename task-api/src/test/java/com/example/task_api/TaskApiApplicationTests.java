package com.example.task_api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("h2")
class TaskApiApplicationTests {
	@Autowired
	private TaskRepository taskRepository;

	@Test
	void contextLoads() {
	}

	@Test
	void migrationCreatesTasksTable() {
		Task task = new Task();
		task.setTitle("Test migration");
		task.setCompleted(false);

		Task saved = taskRepository.save(task);
		assertThat(saved.getId()).isNotNull();
		assertThat(taskRepository.findById(saved.getId())).isPresent();
	}

}
