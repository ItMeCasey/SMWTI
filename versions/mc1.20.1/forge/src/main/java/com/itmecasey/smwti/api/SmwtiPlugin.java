package com.itmecasey.smwti.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as an SMWTI plugin. The class must implement {@link ISmwtiPlugin} and have a public
 * constructor with no parameters; SMWTI finds it by scanning every mod's classes, so a mod needs no
 * call into SMWTI at all and can ship the plugin even when SMWTI is not installed (the annotation
 * is simply never read). Put {@code value} to the id of another mod when the plugin is about that
 * mod's blocks: the plugin is then only loaded when that mod is installed.
 *
 * <pre>{@code
 * @SmwtiPlugin("create")
 * public class CreatePlugin implements ISmwtiPlugin {
 *     @Override
 *     public void register(CommonRegistration registration) { ... }
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SmwtiPlugin {
    /** The id of a mod that must be installed for the plugin to load; empty for none. */
    String value() default "";
}
