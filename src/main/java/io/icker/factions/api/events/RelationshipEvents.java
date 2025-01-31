package io.icker.factions.api.events;

import io.icker.factions.api.persistents.Relationship;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public final class RelationshipEvents {
    public static final Event<NewDecleration> NEW_DECLARATION = EventFactory.createArrayBacked(NewDecleration.class, callbacks -> (claim) -> {
        for (NewDecleration callback : callbacks) {
            callback.onNewDecleration(claim);
        }
    });

    public static final Event<NewMutual> NEW_MUTUAL = EventFactory.createArrayBacked(NewMutual.class, callbacks -> (relationship) -> {
        for (NewMutual callback : callbacks) {
            callback.onNewMutual(relationship);
        }
    });

    public static final Event<EndMutual> END_MUTUAL = EventFactory.createArrayBacked(EndMutual.class, callbacks -> (relationship, oldStatus) -> {
        for (EndMutual callback : callbacks) {
            callback.onEndMutual(relationship, oldStatus);
        }
    });

    @FunctionalInterface
    public interface NewDecleration {
        void onNewDecleration(Relationship relationship);
    }

    @FunctionalInterface
    public interface NewMutual {
        void onNewMutual(Relationship relationship);
    }

    @FunctionalInterface
    public interface EndMutual {
        void onEndMutual(Relationship relationship, Relationship.Status oldStatus);
    }
}
