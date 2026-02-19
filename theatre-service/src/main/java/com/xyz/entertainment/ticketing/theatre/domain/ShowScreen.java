package com.xyz.entertainment.ticketing.theatre.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Maps a show (from movie-service) to a screen in this theatre.
 * One show is played on one screen; seat inventory is per (show, seat).
 */
@Entity
@Table(name = "show_screens")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ShowScreen {

    @Id
    @Column(name = "show_id")
    private Long showId;

    @Column(name = "screen_id", nullable = false)
    private Long screenId;
}
