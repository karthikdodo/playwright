package com.example.playwright.service;

import com.example.playwright.domain.ImageResponse;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface UserRepository extends MongoRepository<ImageResponse, String> {
}