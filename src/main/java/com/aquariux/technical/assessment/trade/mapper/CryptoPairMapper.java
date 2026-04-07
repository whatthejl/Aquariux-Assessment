package com.aquariux.technical.assessment.trade.mapper;

import com.aquariux.technical.assessment.trade.entity.CryptoPair;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CryptoPairMapper {
    
    @Select("""
            SELECT cp.id,
                   cp.base_symbol_id AS baseSymbolId,
                   cp.quote_symbol_id AS quoteSymbolId,
                   cp.pair_name AS pairName,
                   cp.active,
                   bs.symbol AS baseSymbol,
                   qs.symbol AS quoteSymbol
            FROM crypto_pairs cp
            INNER JOIN symbols bs ON bs.id = cp.base_symbol_id
            INNER JOIN symbols qs ON qs.id = cp.quote_symbol_id
            WHERE cp.pair_name = #{pairName}
            """)
    CryptoPair findByPairName(@Param("pairName") String pairName);

    @Select("""
        SELECT id
        FROM crypto_pairs
        WHERE pair_name = #{pairName}
        """)
    Long findIdByPairName(@Param("pairName") String pairName);
}