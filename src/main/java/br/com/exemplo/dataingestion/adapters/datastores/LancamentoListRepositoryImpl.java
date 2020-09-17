package br.com.exemplo.dataingestion.adapters.datastores;

import br.com.exemplo.dataingestion.adapters.datastores.entities.LancamentoEntity;
import br.com.exemplo.dataingestion.adapters.datastores.mappers.EntityMapper;
import br.com.exemplo.dataingestion.adapters.datastores.repositories.LancamentoEntityRepository;
import br.com.exemplo.dataingestion.domain.entities.Lancamento;
import br.com.exemplo.dataingestion.domain.repositories.LancamentoListRepository;
import br.com.exemplo.dataingestion.domain.repositories.LancamentoRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.common.unit.TimeValue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.BulkOptions;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class LancamentoListRepositoryImpl implements LancamentoListRepository {

    private final EntityMapper<Lancamento, LancamentoEntity> lancamentoLancamentoEntityEntityMapper;
    private final EntityMapper<LancamentoEntity,Lancamento> lancamentoEntityLancamentoEntityMapper;
    private final LancamentoEntityRepository lancamentoEntityRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final MeterRegistry simpleMeterRegistry;
    private static final String EXTRATO="extrato";

    @Override
    public List<String> save(List<Lancamento> lancamentoList) {

        List<IndexQuery> collect = lancamentoList.stream()
                .map(lancamento -> lancamentoLancamentoEntityEntityMapper.convert(lancamento))
                .map(lancamentoEntity -> new IndexQueryBuilder()
                        .withId(lancamentoEntity.getNumeroIdentificacaoLancamentoConta().toString())
                        .withObject(lancamentoEntity)
                        .build()
                )
                .collect(Collectors.toList());
        simpleMeterRegistry.counter("elasticsearch.contador","type","save").increment(lancamentoList.size());
        Timer.Sample sample = Timer.start(simpleMeterRegistry);
        List<String> idList = elasticsearchOperations.bulkIndex(collect, BulkOptions.builder().withTimeout(TimeValue.timeValueMillis(5000)).build(), IndexCoordinates.of(EXTRATO));
        sample.stop(simpleMeterRegistry.timer("elasticsearch","type","save","quantidade",String.valueOf(collect.size())));
        return idList;
    }

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
        Page<LancamentoEntity> lancamentoEntityList = lancamentoEntityRepository.findByContaNumeroUnicoConta(idConta, PageRequest.of(0, 10));
        return lancamentoEntityList
                .map(lancamentoEntity -> lancamentoEntityLancamentoEntityMapper.convert(lancamentoEntity))
                .toList();
    }
}
