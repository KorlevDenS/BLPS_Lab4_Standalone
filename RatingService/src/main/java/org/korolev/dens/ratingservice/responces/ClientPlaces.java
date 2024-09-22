package org.korolev.dens.ratingservice.responces;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientPlaces {

    private Integer placeByFame;
    private Integer placeByActivity;

}
