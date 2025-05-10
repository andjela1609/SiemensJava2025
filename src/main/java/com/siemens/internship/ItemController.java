package com.siemens.internship;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    @Autowired
    private ItemService itemService;

    @GetMapping
    public ResponseEntity<List<Item>> getAllItems() {
        return new ResponseEntity<>(itemService.findAll(), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<?> createItem(@Valid @RequestBody Item item, BindingResult result) {
        /*
        if (result.hasErrors()) {
            return new ResponseEntity<>(null, HttpStatus.CREATED);
        }
        return new ResponseEntity<>(itemService.save(item), HttpStatus.BAD_REQUEST);
        */

        if (result.hasErrors()) {
            // If the validation fails --> returns 400 Bad Request
            return new ResponseEntity<>(result.getAllErrors(), HttpStatus.BAD_REQUEST);
        }
        // If the validation is successful --> returns 201 Created
        return new ResponseEntity<>(itemService.save(item), HttpStatus.CREATED);

    }

    @GetMapping("/{id}")
    public ResponseEntity<Item> getItemById(@PathVariable Long id) {
        return itemService.findById(id)
                .map(item -> new ResponseEntity<>(item, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NO_CONTENT));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Item> updateItem(@PathVariable Long id, @RequestBody Item item) {
        /*
        Optional<Item> existingItem = itemService.findById(id);
        if (existingItem.isPresent()) {
            item.setId(id);
            return new ResponseEntity<>(itemService.save(item), HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>(HttpStatus.ACCEPTED);
        }

         */

        Optional<Item> existingItem = itemService.findById(id);
        // If the item is updated --> returns 200 OK
        if (existingItem.isPresent()) {
            item.setId(id);
            return new ResponseEntity<>(itemService.save(item), HttpStatus.OK);
        }
        // If the item doesn't exist --> returns 404 NOT FOUND
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        itemService.deleteById(id);
        //return new ResponseEntity<>(HttpStatus.CONFLICT);
        // Returns proper deletion response --> 204 NO CONTENT
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // The async method from the service returns a CompletableFuture,
    // thus the controller must return CompletableFuture<ResponseEntity<List<Item>>>
    @GetMapping("/process")
    public CompletableFuture<ResponseEntity<List<Item>>> processItems() {
        return itemService.processItemsAsync()
                .thenApply(items -> new ResponseEntity<>(items, HttpStatus.OK));
    }

}
