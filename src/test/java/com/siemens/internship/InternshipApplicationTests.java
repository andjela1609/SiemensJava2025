package com.siemens.internship;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;





@SpringBootTest
class InternshipApplicationTests {

	@Test
	void contextLoads() {
	}

}

@WebMvcTest(ItemController.class)
class ItemControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private ItemService itemService;

	@Test
	void testGetAllItems() throws Exception {
		List<Item> items = List.of(new Item(1L, "Item1", "Desc", "NEW", "email@test.com"));
		Mockito.when(itemService.findAll()).thenReturn(items);

		mockMvc.perform(get("/api/items"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void testCreateItemValid() throws Exception {
		Item item = new Item(null, "Name", "Desc", "NEW", "email@test.com");
		ObjectMapper mapper = new ObjectMapper();
		Mockito.when(itemService.save(Mockito.any())).thenReturn(item);

		mockMvc.perform(post("/api/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content(mapper.writeValueAsString(item)))
				.andExpect(status().isCreated());
	}

	@Test
	void testCreateItemInvalidEmail() throws Exception {
		Item item = new Item(null, "Name", "Desc", "NEW", "invalid-email");
		ObjectMapper mapper = new ObjectMapper();

		mockMvc.perform(post("/api/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content(mapper.writeValueAsString(item)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void testGetItemByIdFound() throws Exception {
		Item item = new Item(1L, "Item1", "Desc", "NEW", "email@test.com");
		Mockito.when(itemService.findById(1L)).thenReturn(Optional.of(item));

		mockMvc.perform(get("/api/items/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Item1"));
	}

	@Test
	void testGetItemByIdNotFound() throws Exception {
		Mockito.when(itemService.findById(2L)).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/items/2"))
				.andExpect(status().isNoContent());
	}

	@Test
	void testDeleteItem() throws Exception {
		mockMvc.perform(delete("/api/items/1"))
				.andExpect(status().isConflict());
	}

	@Test
	void testProcessItems() throws Exception {
		List<Item> processed = List.of(new Item(1L, "Item", "Desc", "PROCESSED", "test@email.com"));
		Mockito.when(itemService.processItemsAsync())
				.thenReturn(CompletableFuture.completedFuture(processed));

		mockMvc.perform(get("/api/items/process"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].status").value("PROCESSED"));
	}
}

@SpringBootTest
class ItemServiceTest {

	@Autowired
	private ItemService itemService;

	@MockBean
	private ItemRepository itemRepository;

	@Test
	void testFindAll() {
		List<Item> items = List.of(new Item(1L, "Item1", "Desc", "NEW", "email@test.com"));
		Mockito.when(itemRepository.findAll()).thenReturn(items);

		List<Item> result = itemService.findAll();
		assertEquals(1, result.size());
	}

	@Test
	void testFindByIdExists() {
		Item item = new Item(1L, "Item1", "Desc", "NEW", "email@test.com");
		Mockito.when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

		Optional<Item> result = itemService.findById(1L);
		assertTrue(result.isPresent());
		assertEquals("Item1", result.get().getName());
	}

	@Test
	void testSave() {
		Item item = new Item(null, "Saved", "Desc", "NEW", "test@test.com");
		Mockito.when(itemRepository.save(item)).thenReturn(item);

		Item result = itemService.save(item);
		assertEquals("Saved", result.getName());
	}

	@Test
	void testProcessItemsAsync() throws Exception {
		List<Long> ids = List.of(1L);
		Item item = new Item(1L, "ProcessMe", "Desc", "NEW", "email@test.com");

		Mockito.when(itemRepository.findAllIds()).thenReturn(ids);
		Mockito.when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
		Mockito.when(itemRepository.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));

		CompletableFuture<List<Item>> future = itemService.processItemsAsync();
		List<Item> result = future.get(2, TimeUnit.SECONDS);

		assertEquals(1, result.size());
		assertEquals("PROCESSED", result.get(0).getStatus());
	}
}

@DataJpaTest
class ItemRepositoryTest {

	@Autowired
	private ItemRepository itemRepository;

	@Test
	void testSaveAndFindById() {
		Item item = new Item(null, "RepoItem", "Desc", "NEW", "test@repo.com");
		Item saved = itemRepository.save(item);

		Optional<Item> found = itemRepository.findById(saved.getId());
		assertTrue(found.isPresent());
		assertEquals("RepoItem", found.get().getName());
	}

	@Test
	void testFindAllIds() {
		Item item1 = new Item(null, "Item1", "Desc", "NEW", "one@test.com");
		Item item2 = new Item(null, "Item2", "Desc", "NEW", "two@test.com");

		itemRepository.save(item1);
		itemRepository.save(item2);

		List<Long> ids = itemRepository.findAllIds();
		assertEquals(2, ids.size());
	}

	@Test
	void testDeleteById() {
		Item item = new Item(null, "ToDelete", "Desc", "NEW", "delete@test.com");
		Item saved = itemRepository.save(item);
		itemRepository.deleteById(saved.getId());

		assertFalse(itemRepository.findById(saved.getId()).isPresent());
	}
}


