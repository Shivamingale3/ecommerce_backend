package com.shivamingale.ecom.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shivamingale.ecom.dto.request.TempMediaDTO;
import com.shivamingale.ecom.dto.response.AppResponse;
import com.shivamingale.ecom.entity.Media;
import com.shivamingale.ecom.service.MediaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/media")
public class AdminMediaController {

    @Autowired
    private MediaService mediaService;

    @PutMapping("/upload/temp")
    public ResponseEntity<AppResponse<Media>> uploadTempMedia(@Valid @ModelAttribute TempMediaDTO tempMediaDTO) {
        return ResponseEntity.ok()
                .body(AppResponse.success(mediaService.uploadTempMedia(tempMediaDTO),
                        "Media uploaded successfully", HttpStatus.OK));
    }

}
