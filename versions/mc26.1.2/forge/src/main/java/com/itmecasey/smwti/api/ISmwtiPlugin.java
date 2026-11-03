package com.itmecasey.smwti.api;

/**
 * The entry point of an SMWTI plugin; see {@link SmwtiPlugin}. SMWTI creates one instance per
 * plugin class and calls these once while the game loads: {@link #register} on both the client and
 * a dedicated server, {@link #registerClient} on the client only.
 *
 * <p>Things that need the screen (client providers, overrides, elements, settings) go in
 * {@link #registerClient}, so a dedicated server never loads their classes. Things the server works
 * out go in {@link #register}.
 */
public interface ISmwtiPlugin {
    /** Registers what runs on the server, and on the client where it only needs to exist. */
    default void register(CommonRegistration registration) {
    }

    /** Registers what draws on the client. */
    default void registerClient(ClientRegistration registration) {
    }
}
