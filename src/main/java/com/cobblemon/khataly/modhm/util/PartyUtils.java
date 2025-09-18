package com.cobblemon.khataly.modhm.util;


import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.RenderablePokemon;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;


public class PartyUtils {
    /**
     * Ritorna se il party contiene la move
     */
    public static boolean hasMove(ServerPlayerEntity player,String hm) {
        MoveTemplate moveToFind = Moves.INSTANCE.getByName(hm);
        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);

        for (Pokemon pokemon : party) {
            for (Move move : pokemon.getMoveSet().getMoves()) {
                if (move.getTemplate() == moveToFind) {
                    return true;
                }
            }
        }

        return false;
    }

    public static RenderablePokemon getRenderPokemonByMove(ServerPlayerEntity player,String hm) {
        MoveTemplate HM = Moves.INSTANCE.getByName(hm);
        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);

        for (Pokemon pokemon : party) {
            for (Move move : pokemon.getMoveSet().getMoves()) {
                if (move.getTemplate() == HM) {
                    return pokemon.asRenderablePokemon();
                }
            }
        }
        return null;
    }

    /**
     * Ritorna se il pokemon che conosce hm specifica
     */
    public static Boolean pokemonHasMoveToGUI(ServerPlayerEntity player, UUID pokemonId,String hm) {
        MoveTemplate hmToFind = Moves.INSTANCE.getByName(hm);
        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);

        for (Pokemon pokemon : party) {
            if (!pokemon.getUuid().equals(pokemonId)) continue;

            for (Move move : pokemon.getMoveSet().getMoves()) {
                if (move.getTemplate() == hmToFind) {
                    return true;
                }
            }
        }
        return false;
    }


}