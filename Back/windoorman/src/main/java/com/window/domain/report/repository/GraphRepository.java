package com.window.domain.report.repository;

import com.window.domain.report.entity.Graph;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface GraphRepository extends ElasticsearchRepository<Graph, String> {
}
