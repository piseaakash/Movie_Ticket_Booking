package com.xyz.entertainment.ticketing.theatre.api;

import com.xyz.entertainment.ticketing.theatre.api.model.CreateScreenRequest;
import com.xyz.entertainment.ticketing.theatre.api.model.CreateTheatreRequest;
import com.xyz.entertainment.ticketing.theatre.api.model.ScreenResponse;
import com.xyz.entertainment.ticketing.theatre.api.model.TheatreResponse;
import com.xyz.entertainment.ticketing.theatre.api.model.UpdateScreenRequest;
import com.xyz.entertainment.ticketing.theatre.api.model.UpdateTheatreRequest;
import com.xyz.entertainment.ticketing.theatre.service.ScreenService;
import com.xyz.entertainment.ticketing.theatre.service.TheatreService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Delegate implementation that bridges generated API layer to {@link TheatreService} and {@link ScreenService}.
 */
@Component
@Primary
@RequiredArgsConstructor
public class TheatresApiDelegateImpl implements TheatresApiDelegate {

    private final TheatreService theatreService;
    private final ScreenService screenService;

    @Override
    public ResponseEntity<TheatreResponse> createTheatre(CreateTheatreRequest createTheatreRequest) {
        TheatreResponse response = theatreService.createTheatre(createTheatreRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<Void> deleteTheatre(Long id) {
        theatreService.deleteTheatre(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<TheatreResponse> getTheatre(Long id) {
        TheatreResponse response = theatreService.getTheatre(id);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<List<TheatreResponse>> listTheatres() {
        List<TheatreResponse> list = theatreService.listTheatres();
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<TheatreResponse> updateTheatre(Long id, UpdateTheatreRequest updateTheatreRequest) {
        TheatreResponse response = theatreService.updateTheatre(id, updateTheatreRequest);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ScreenResponse> createScreen(Long theatreId, CreateScreenRequest createScreenRequest) {
        ScreenResponse response = screenService.createScreen(theatreId, createScreenRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<Void> deleteScreen(Long theatreId, Long screenId) {
        screenService.deleteScreen(theatreId, screenId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ScreenResponse> getScreen(Long theatreId, Long screenId) {
        ScreenResponse response = screenService.getScreen(theatreId, screenId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<List<ScreenResponse>> listScreens(Long theatreId) {
        List<ScreenResponse> list = screenService.listScreens(theatreId);
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<ScreenResponse> updateScreen(Long theatreId, Long screenId, UpdateScreenRequest updateScreenRequest) {
        ScreenResponse response = screenService.updateScreen(theatreId, screenId, updateScreenRequest);
        return ResponseEntity.ok(response);
    }
}

