package com.vaadin.flow.renderer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.data.provider.CompositeDataGenerator;
import com.vaadin.flow.data.provider.DataGenerator;
import com.vaadin.flow.data.provider.DataKeyMapper;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.renderer.Rendering;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.internal.JsonSerializer;
import com.vaadin.flow.internal.JsonUtils;
import com.vaadin.flow.internal.nodefeature.ReturnChannelMap;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;

import elemental.json.JsonArray;

@JsModule("./lit-renderer.ts")
public class LitRenderer<T> extends Renderer<T> {
    private String templateExpression;

    private final String DEFAULT_RENDERER_NAME = "renderer";

    private final String propertyNamespace = UUID.randomUUID().toString() + "_";

    private Map<String, SerializableBiConsumer<T, JsonArray>> clientCallables = new HashMap<>();

    private LitRenderer(String templateExpression) {
        this.templateExpression = templateExpression;
    }

    @Override
    public Rendering<T> render(Element container, DataKeyMapper<T> keyMapper,
            Element contentTemplate) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Rendering<T> render(Element container, DataKeyMapper<T> keyMapper) {
        return this.render(container, keyMapper, DEFAULT_RENDERER_NAME);
    }

    public Rendering<T> render(Element container, DataKeyMapper<T> keyMapper,
            String rendererName) {
        ReturnChannelRegistration returnChannel = container.getNode()
                .getFeature(ReturnChannelMap.class)
                .registerChannel(arguments -> {
                    String handlerName = arguments.getString(0);
                    String itemKey = arguments.getString(1);
                    JsonArray args = arguments.getArray(2);

                    SerializableBiConsumer<T, JsonArray> handler = clientCallables
                            .get(handlerName);
                    T item = keyMapper.get(itemKey);

                    handler.accept(item, args);
                });

        JsonArray clientCallablesArray = JsonUtils.listToJson(
                clientCallables.keySet().stream().collect(Collectors.toList()));

        container.executeJs(
                "window.Vaadin.setLitRenderer(this, $0, $1, $2, $3, $4)",
                rendererName, templateExpression, returnChannel,
                clientCallablesArray, propertyNamespace);

        return new Rendering<T>() {
            @Override
            public Optional<DataGenerator<T>> getDataGenerator() {
                Map<String, ValueProvider<T, ?>> valueProviders = getValueProviders();
                if (valueProviders == null || valueProviders.isEmpty()) {
                    return Optional.empty();
                }
                CompositeDataGenerator<T> composite = new CompositeDataGenerator<>();

                valueProviders.forEach((key, provider) -> composite
                        .addDataGenerator((item, jsonObject) -> jsonObject.put(
                                key,
                                JsonSerializer.toJson(provider.apply(item)))));

                return Optional.of(composite);
            }

            @Override
            public Element getTemplateElement() {
                return null;
            }
        };
    }

    public LitRenderer<T> withProperty(String property,
            ValueProvider<T, ?> provider) {
        // Prefix the property name with a LitRenderer instance specific
        // namespace to avoid property name clashes.
        // Fixes https://github.com/vaadin/flow/issues/8629 in LitRenderer
        setProperty(propertyNamespace + property, provider);
        return this;
    }

    public LitRenderer<T> withClientCallable(String functionName,
            SerializableConsumer<T> handler) {
        return withClientCallable(functionName,
                (item, ignore) -> handler.accept(item));
    }

    public LitRenderer<T> withClientCallable(String functionName,
            SerializableBiConsumer<T, JsonArray> handler) {
        // TODO validate functionName
        clientCallables.put(functionName, handler);
        return this;
    }

    public static <T> LitRenderer<T> of(String templateExpression) {
        return new LitRenderer<>(templateExpression);
    }
}