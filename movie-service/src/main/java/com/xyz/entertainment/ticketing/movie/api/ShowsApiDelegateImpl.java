package com.xyz.entertainment.ticketing.movie.api;

import com.xyz.entertainment.ticketing.movie.api.model.CreateShowRequest;
import com.xyz.entertainment.ticketing.movie.api.model.ShowResponse;
import com.xyz.entertainment.ticketing.movie.api.model.UpdateShowRequest;
import com.xyz.entertainment.ticketing.movie.service.ShowManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@RequiredArgsConstructor
public class ShowsApiDelegateImpl implements ShowsApiDelegate {

    private final ShowManagementService showManagementService;

    @Override
    public ResponseEntity<ShowResponse> createShow(CreateShowRequest createShowRequest) {
        ShowResponse body = showManagementService.createShow(createShowRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @Override
    public ResponseEntity<ShowResponse> updateShow(Long id, UpdateShowRequest updateShowRequest) {
        ShowResponse body = showManagementService.updateShow(id, updateShowRequest);
        return ResponseEntity.ok(body);
    }

    @Override
    public ResponseEntity<Void> deleteShow(Long id) {
        showManagementService.deleteShow(id);
        return ResponseEntity.noContent().build();
    }
}
