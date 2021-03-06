package com.example;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import brave.context.slf4j.MDCScopeDecorator;
import brave.propagation.B3Propagation;
import brave.propagation.ExtraFieldPropagation;
import brave.propagation.ThreadLocalCurrentTraceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zipkin2.codec.SpanBytesEncoder;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Sender;
import zipkin2.reporter.okhttp3.OkHttpSender;

import java.util.concurrent.TimeUnit;

public class TraceDemo {

    private static Logger logger = LoggerFactory.getLogger(TraceDemo.class);

    public static Tracing initTracing() {
        Sender sender = OkHttpSender.create("http://localhost:9411/api/v2/spans");

        AsyncReporter asyncReporter = AsyncReporter.builder(sender)
                .closeTimeout(500, TimeUnit.MILLISECONDS)
                .build(SpanBytesEncoder.JSON_V2);

        Tracing tracing = Tracing.newBuilder()
                .localServiceName("tracer-demo")
                .spanReporter(asyncReporter)
                .propagationFactory(ExtraFieldPropagation.newFactory(B3Propagation.FACTORY, "user-name"))
                //.currentTraceContext(ThreadContextCurrentTraceContext.create())
                .currentTraceContext(ThreadLocalCurrentTraceContext.newBuilder()
                        .addScopeDecorator(MDCScopeDecorator.create()) // puts trace IDs into logs
                        .build()
                )
                .build();

        return tracing;
    }

    public static void main(String[] args) {
        Tracing tracing = initTracing();
        testTraceNormal(tracing);
        //testTraceTwoPhase(tracing);
        //testTraceTwoPhase2(tracing);
        sleep(1000);
    }


    private static void testTraceNormal(Tracing tracing) {
        Tracer tracer = tracing.tracer();
        Span span = tracer.newTrace().name("encode2").start();
        try {
            System.out.println("i am testing now.");
            logger.info("i am testing now.");
            doSomethingExpensive();
        } finally {
            span.finish();
        }
    }

    private static void testTraceTwoPhase(Tracing tracing) {
        Tracer tracer = tracing.tracer();
        Span twoPhase = tracer.newTrace().name("twoPhase").start();
        try {
            Span prepare = tracer.newChild(twoPhase.context()).name("prepare").start();
            try {
                prepare();
            } finally {
                prepare.finish();
            }
            Span commit = tracer.newChild(twoPhase.context()).name("commit").start();
            try {
                commit();
            } finally {
                commit.finish();
            }
        } finally {
            twoPhase.finish();
        }
    }

    private static void testTraceTwoPhase2(Tracing tracing) {
        Tracer tracer = tracing.tracer();
        Span twoPhase = tracer.newTrace().name("twoPhase").start();
        try {
            Span prepare = tracer.newChild(twoPhase.context()).name("prepare").start();
            try {
                prepare2Step();
            } finally {
                prepare.finish();
            }
            Span commit = tracer.newChild(twoPhase.context()).name("commit").start();
            try {
                commit();
            } finally {
                commit.finish();
            }
        } finally {
            twoPhase.finish();
        }
    }

    private static void doSomethingExpensive() {
        sleep(500);
    }

    private static void commit() {
        sleep(500);
    }

    private static void prepare() {
        sleep(500);
    }

    private static void prepare2Step() {
        Tracing tracing = Tracing.current();
//        tracing.

        sleep(500);
    }

    private static void sleep(long milliseconds) {
        try {
            TimeUnit.MILLISECONDS.sleep(milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
