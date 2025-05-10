package com.siemens.internship;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class ItemService {
    @Autowired
    private ItemRepository itemRepository;

    private static ExecutorService executor = Executors.newFixedThreadPool(10);

    // ArrayList as well as int --> not thread-safe
    // We should allow safe access from threads
    private final List<Item> processedItems = new CopyOnWriteArrayList<>();
    private final AtomicInteger processedCount = new AtomicInteger(0);


    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    public Optional<Item> findById(Long id) {
        return itemRepository.findById(id);
    }

    public Item save(Item item) {
        return itemRepository.save(item);
    }

    public void deleteById(Long id) {
        itemRepository.deleteById(id);
    }


    /**
     * Your Tasks
     * Identify all concurrency and asynchronous programming issues in the code
     * Fix the implementation to ensure:
     * All items are properly processed before the CompletableFuture completes
     * Thread safety for all shared state
     * Proper error handling and propagation
     * Efficient use of system resources
     * Correct use of Spring's @Async annotation
     * Add appropriate comments explaining your changes and why they fix the issues
     * Write a brief explanation of what was wrong with the original implementation
     *
     * Hints
     * Consider how CompletableFuture composition can help coordinate multiple async operations
     * Think about appropriate thread-safe collections
     * Examine how errors are handled and propagated
     * Consider the interaction between Spring's @Async and CompletableFuture
     */

    // We should handle this method asynchronously so this method should return CompletableFuture<>
    // CompletableFuture<> waits for background tasks to complete
    @Async
    public CompletableFuture<List<Item>> processItemsAsync() {

        List<Long> itemIds = itemRepository.findAllIds();

        /*

        for (Long id : itemIds) {
            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(100);

                    Item item = itemRepository.findById(id).orElse(null);
                    if (item == null) {
                        return;
                    }

                    processedCount++;

                    item.setStatus("PROCESSED");
                    itemRepository.save(item);
                    processedItems.add(item);

                } catch (InterruptedException e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }, executor);
        }

        return processedItems;
         */

        // Each item should be added to this list
        List<CompletableFuture<Void>> futures = itemIds.stream()
                .map(id -> CompletableFuture.runAsync(() -> {
                    try {
                        // Simulate processing delay
                        Thread.sleep(100);

                        itemRepository.findById(id).ifPresent(item -> {
                            item.setStatus("PROCESSED");
                            itemRepository.save(item);

                            processedItems.add(item);
                            processedCount.incrementAndGet();
                        });
                    } catch (Exception e) {
                        // Handle error more robustly
                        System.err.println("Error processing item ID " + id + ": " + e.getMessage());
                        throw new CompletionException(e);
                    }
                }))
                .toList();

        // We should wait for all tasks to be complete and then return the final list
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> processedItems);
    }
    }



