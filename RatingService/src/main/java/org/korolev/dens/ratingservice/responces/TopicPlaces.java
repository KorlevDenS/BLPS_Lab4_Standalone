package org.korolev.dens.ratingservice.responces;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopicPlaces {

    private Integer placeByViews;
    private Integer placeByFame;

}
