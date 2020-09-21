package br.com.exemplo.dataingestion.adapters.datastores;

import br.com.exemplo.dataingestion.adapters.datastores.entities.LancamentoEntity;
import br.com.exemplo.dataingestion.adapters.datastores.mappers.EntityMapper;
import br.com.exemplo.dataingestion.adapters.datastores.repositories.LancamentoEntityRepository;
import br.com.exemplo.dataingestion.domain.entities.Lancamento;
import br.com.exemplo.dataingestion.domain.repositories.LancamentoRepository;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryShardContext;
import org.joda.time.format.DateTimeFormat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class LancamentoRepositoryImpl implements LancamentoRepository {

    private final EntityMapper<Lancamento, LancamentoEntity> lancamentoLancamentoEntityEntityMapper;
    private final EntityMapper<LancamentoEntity,Lancamento> lancamentoEntityLancamentoEntityMapper;
    private final LancamentoEntityRepository lancamentoEntityRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    @Override
    public Lancamento getLancamento(UUID id) {
        return lancamentoEntityLancamentoEntityMapper
                .convert(
                        lancamentoEntityRepository.findById(id)
                                .orElse(
                                        LancamentoEntity.builder()
                                                .build()
                                )
                );
    }

    @Override
    public List<Lancamento> getLancamentosByConta(UUID idConta) {
        Page<LancamentoEntity> lancamentoEntityList = lancamentoEntityRepository.findByContaNumeroUnicoConta(idConta, PageRequest.of(0, 100));
        return lancamentoEntityList
                .map(lancamentoEntity -> lancamentoEntityLancamentoEntityMapper.convert(lancamentoEntity))
                .toList();
    }

    @Override
    public List<Lancamento> getLancamentosByData(UUID idConta, OffsetDateTime dataInicio, OffsetDateTime dataFim) {
        SearchHits<LancamentoEntity> dataLancamento = elasticsearchOperations.search(
                new NativeSearchQueryBuilder()
                        .withQuery(QueryBuilders.rangeQuery("dataLancamento")
                                .gte(dataInicio.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                                .lte(dataFim.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)))
                        .withPageable(Pageable.unpaged())
                        .build(), LancamentoEntity.class);
        return dataLancamento.stream()
                .map(lancamentoEntitySearchHit ->
                        lancamentoEntityLancamentoEntityMapper.convert(lancamentoEntitySearchHit.getContent())
                )
                .collect(Collectors.toList());

    }
}
