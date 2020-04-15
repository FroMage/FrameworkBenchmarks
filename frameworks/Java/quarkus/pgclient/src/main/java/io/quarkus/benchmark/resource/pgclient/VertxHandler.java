package io.quarkus.benchmark.resource.pgclient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.core.ServerResponseWriter;
import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.core.interception.jaxrs.ContainerResponseContextImpl;
import org.jboss.resteasy.core.interception.jaxrs.ResponseContainerRequestContext;
import org.jboss.resteasy.plugins.providers.StringTextStar;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;
import org.jboss.resteasy.specimpl.BuiltResponse;
import org.jboss.resteasy.specimpl.ResteasyHttpHeaders;
import org.jboss.resteasy.specimpl.ResteasyUriInfo;
import org.jboss.resteasy.spi.AsyncMessageBodyWriter;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.benchmark.filter.ServerHeaderFilter;
import io.quarkus.benchmark.model.World;
import io.quarkus.benchmark.resource.JsonResource;
import io.quarkus.benchmark.resource.PlaintextResource;
import io.quarkus.resteasy.runtime.standalone.BufferAllocator;
import io.quarkus.resteasy.runtime.standalone.ResteasyStandaloneRecorder;
import io.quarkus.resteasy.runtime.standalone.VertxBlockingOutput;
import io.quarkus.resteasy.runtime.standalone.VertxHttpRequest;
import io.quarkus.resteasy.runtime.standalone.VertxHttpResponse;
import io.quarkus.resteasy.runtime.standalone.VertxUtil;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.http.runtime.VertxInputStream;
import io.vertx.core.Context;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class VertxHandler {
    
    @Inject
    JsonResource jsonResource = new JsonResource(); // don't ask, no idea why Arc injects plaintext but not this one

    @Inject
    PlaintextResource plaintextResource;

    @Inject
    DbResource dbResource;

    @Inject
    FortuneResource fortuneResource;

    @Inject
    ServerHeaderFilter serverHeaderFilter;

    protected static final int BUFFER_SIZE = 8 * 1024;

    private static final Annotation[] NO_ANNOTATIONS = new Annotation[0];

    private static boolean useDirect = true;

    //TODO: clean this up
    private static BufferAllocator ALLOCATOR = new BufferAllocator() {
        @Override
        public ByteBuf allocateBuffer() {
            return allocateBuffer(useDirect);
        }

        @Override
        public ByteBuf allocateBuffer(boolean direct) {
            return allocateBuffer(direct, BUFFER_SIZE);
        }

        @Override
        public ByteBuf allocateBuffer(int bufferSize) {
            return allocateBuffer(useDirect, bufferSize);
        }

        @Override
        public ByteBuf allocateBuffer(boolean direct, int bufferSize) {
            if (direct) {
                return PooledByteBufAllocator.DEFAULT.directBuffer(bufferSize);
            } else {
                return PooledByteBufAllocator.DEFAULT.heapBuffer(bufferSize);
            }
        }

        @Override
        public int getBufferSize() {
            return BUFFER_SIZE;
        }
    };

    private long readTimeout = 60 * 1000; // default, from config
    private String rootPath = "/";
    private ResteasyDeployment deployment;

    private AsyncMessageBodyWriter textWriter = new StringTextStar();
    private AsyncMessageBodyWriter jsonWriter = new ResteasyJackson2Provider();
    private static Type ListOfWorlds;
    static {
        try {
            ListOfWorlds = VertxHandler.class.getDeclaredMethod("listOfWorldsType").getGenericReturnType();
        } catch (NoSuchMethodException | SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    // don't ask
    private static List<World> listOfWorldsType(){ return null; }
    
    public void init(@Observes StartupEvent e, Router router) {
//        try {
//            Field field = ResteasyStandaloneRecorder.class.getDeclaredField("deployment");
//            field.setAccessible(true);
//            deployment = (ResteasyDeployment) field.get(null);
//        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e1) {
//            // TODO Auto-generated catch block
//            e1.printStackTrace();
//        }
//        router.get("/plaintext").handler(rc -> handlePlaintext(rc));
//        router.get("/json").handler(rc -> handleJson(rc));
//        router.get("/db").handler(rc -> handleDb(rc));
//        router.get("/updates").handler(rc -> handleUpdates(rc));
//        router.get("/queries").handler(rc -> handleQueries(rc));
//        router.get("/fortunes").handler(rc -> handleFortunes(rc));
    }

    private Function<UriInfo, CompletionStage<BuiltResponse>> textInvoker = new Function<>() {
        public CompletionStage<BuiltResponse> apply(UriInfo uriInfo) {
            String ret = plaintextResource.plaintext();
            return CompletableFuture.completedFuture((BuiltResponse) Response.ok(ret, MediaType.TEXT_PLAIN).build());
        }
    };
    
    private void handlePlaintext(RoutingContext routingContext) {
        handleRequest(routingContext, textInvoker, textWriter , String.class, String.class, NO_ANNOTATIONS);
    }

    private Function<UriInfo, CompletionStage<BuiltResponse>> jsonInvoker = new Function<>() {
        public CompletionStage<BuiltResponse> apply(UriInfo uriInfo) {
            Map<String, String> ret = jsonResource.json();
            return CompletableFuture.completedFuture((BuiltResponse) Response.ok(ret, MediaType.APPLICATION_JSON).build());
        }
    };

    private void handleJson(RoutingContext routingContext) {
        handleRequest(routingContext, jsonInvoker, jsonWriter, Map.class, Map.class, NO_ANNOTATIONS);
    }

    private Function<UriInfo, CompletionStage<BuiltResponse>> dbInvoker = new Function<>() {
        public CompletionStage<BuiltResponse> apply(UriInfo uriInfo) {
            CompletionStage<World> ret = dbResource.db();
            return ret.thenApply(world -> (BuiltResponse) Response.ok(world, MediaType.APPLICATION_JSON).build());
        }
    };
    
    private void handleDb(RoutingContext routingContext) {
        handleRequest(routingContext, dbInvoker, jsonWriter , World.class, World.class, NO_ANNOTATIONS);
    }

    private Function<UriInfo, CompletionStage<BuiltResponse>> queriesInvoker = new Function<>() {
        public CompletionStage<BuiltResponse> apply(UriInfo uriInfo) {
            CompletionStage<List<World>> ret = dbResource.queries(uriInfo.getQueryParameters().getFirst("queries"));
            return ret.thenApply(world -> (BuiltResponse) Response.ok(world, MediaType.APPLICATION_JSON).build());
        }
    };
    
    private void handleQueries(RoutingContext routingContext) {
        handleRequest(routingContext, queriesInvoker, jsonWriter, List.class, ListOfWorlds, NO_ANNOTATIONS);
    }

    private Function<UriInfo, CompletionStage<BuiltResponse>> updatesInvoker = new Function<>() {
        public CompletionStage<BuiltResponse> apply(UriInfo uriInfo) {
            CompletionStage<List<World>> ret = dbResource.updates(uriInfo.getQueryParameters().getFirst("queries"));
            return ret.thenApply(world -> (BuiltResponse) Response.ok(world, MediaType.APPLICATION_JSON).build());
        }
    };
    
    private void handleUpdates(RoutingContext routingContext) {
        handleRequest(routingContext, updatesInvoker, jsonWriter, List.class, ListOfWorlds, NO_ANNOTATIONS);
    }

    private Function<UriInfo, CompletionStage<BuiltResponse>> fortunesInvoker = new Function<>() {
        public CompletionStage<BuiltResponse> apply(UriInfo uriInfo) {
            CompletionStage<String> ret = fortuneResource.fortunes();
            return ret.thenApply(world -> (BuiltResponse) Response.ok(world, MediaType.TEXT_HTML_TYPE.withCharset("UTF-8")).build());
        }
    };
    
    private void handleFortunes(RoutingContext routingContext) {
        handleRequest(routingContext, fortunesInvoker, textWriter, String.class, String.class, NO_ANNOTATIONS);
    }

    private void handleRequest(RoutingContext routingContext, 
                               Function<UriInfo, CompletionStage<BuiltResponse>> invoker, 
                               AsyncMessageBodyWriter writer,
                               Class<?> returnClass, Type returnType, Annotation[] methodAnnotations) {
        HttpServerRequest request = routingContext.request();
        InputStream is;
        try {
            if (routingContext.getBody() != null) {
                is = new ByteArrayInputStream(routingContext.getBody().getBytes());
            } else {
                is = new VertxInputStream(routingContext, readTimeout );
            }
        } catch (IOException e) {
            routingContext.fail(e);
            return;
        }
        VertxBlockingOutput output = new VertxBlockingOutput(routingContext.request());

        ManagedContext cdiRequestContext = Arc.container().requestContext();
        cdiRequestContext.activate();
        
        Context ctx = routingContext.vertx().getOrCreateContext();
        ResteasyUriInfo uriInfo = VertxUtil.extractUriInfo(request, rootPath);
        ResteasyHttpHeaders headers = VertxUtil.extractHttpHeaders(request);
        SynchronousDispatcher dispatcher = null; // FIXME?
        ResteasyProviderFactory providerFactory = deployment.getProviderFactory();
        VertxHttpResponse resteasyHttpResponse = new VertxHttpResponse(request, providerFactory,
                request.method(), ALLOCATOR, output);

        // using a supplier to make the remote Address resolution lazy: often it's not needed and it's not very cheap to create.
        // FIXME: access
//        LazyHostSupplier hostSupplier = new LazyHostSupplier(request);

        VertxHttpRequest resteasyHttpRequest = new VertxHttpRequest(ctx, routingContext, headers, uriInfo, request.rawMethod(),
                null /* FIXME: access */,
                dispatcher, resteasyHttpResponse, cdiRequestContext);
        resteasyHttpRequest.setInputStream(is);

        invoker.apply(uriInfo)
        .exceptionally(t -> {
            t.printStackTrace();
            return null;
        })
        .thenAccept(builtResponse -> {
            ContainerResponseFilter[] responseFilters = new ContainerResponseFilter[] {serverHeaderFilter};
            ResponseContainerRequestContext requestContext = null; // not used
            ContainerResponseContextImpl responseContext = new ContainerResponseContextImpl(resteasyHttpRequest, resteasyHttpResponse, builtResponse, 
               requestContext, responseFilters, 
               t -> {
                   // onComplete
                   if(t != null) {
                       t.printStackTrace();
                   }
               }, 
               v -> {
                   // continuation
                   ServerResponseWriter.commitHeaders(builtResponse, resteasyHttpResponse);
                   writer.asyncWriteTo(builtResponse.getEntity(), returnClass, returnType, methodAnnotations, builtResponse.getMediaType(), 
                                       (MultivaluedMap)resteasyHttpRequest.getMutableHeaders(), 
                                       resteasyHttpResponse.getAsyncOutputStream()).thenAccept(v2 -> {
                                           // FIXME: conditional on async
                                           cdiRequestContext.deactivate();
                                           try {
                                               resteasyHttpResponse.finish();
                                           } catch (IOException e) {
                                               // TODO Auto-generated catch block
                                               e.printStackTrace();
                                           }
                                       }).exceptionally(t -> {
                                           ((Throwable)t).printStackTrace();
                                           return null;
                                       });
               }) ;
            try {
                responseContext.filter();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

}
