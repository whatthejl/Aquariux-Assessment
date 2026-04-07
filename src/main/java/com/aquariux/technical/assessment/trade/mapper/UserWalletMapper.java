package com.aquariux.technical.assessment.trade.mapper;

import com.aquariux.technical.assessment.trade.dto.internal.UserWalletDto;
import com.aquariux.technical.assessment.trade.entity.UserWallet;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserWalletMapper {
    
    @Select("""
            SELECT s.symbol, s.name, uw.balance 
            FROM symbols s 
            INNER JOIN user_wallets uw ON s.id = uw.symbol_id AND uw.user_id = #{userId} 
            ORDER BY s.symbol
            """)
    List<UserWalletDto> findByUserId(Long userId);

    @Select("""
            SELECT id,
                   user_id AS userId,
                   symbol_id AS symbolId,
                   balance,
                   updated_at AS updatedAt
            FROM user_wallets
            WHERE user_id = #{userId}
              AND symbol_id = #{symbolId}
            """)
    UserWallet findWalletByUserIdAndSymbolId(@Param("userId") Long userId, @Param("symbolId") Long symbolId);

    @Update("""
            UPDATE user_wallets
            SET balance = #{balance},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
            """)
    int updateWalletBalance(UserWallet userWallet);

    @Insert("""
            INSERT INTO user_wallets (user_id, symbol_id, balance)
            VALUES (#{userId}, #{symbolId}, #{balance})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertWallet(UserWallet userWallet);

}