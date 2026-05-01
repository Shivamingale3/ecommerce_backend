package com.shivamingale.ecom.repository;

import com.shivamingale.ecom.entity.Media;
import com.shivamingale.ecom.enums.MediaType;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface MediaRepository extends BaseRepository<Media> {

    List<Media> findByTypeAndDeletedFalse(MediaType type);

    List<Media> findByActiveTrueAndDeletedFalse();
}