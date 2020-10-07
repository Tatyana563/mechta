package com.example.mechta.repository;

import com.example.mechta.model.MainGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MainGroupRepository extends JpaRepository<MainGroup, Integer> {
    Optional<MainGroup> findOneByUrl(String url);
}
