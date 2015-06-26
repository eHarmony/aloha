package com.eharmony.aloha.semantics.compiled.plugin.proto;

//import com.eharmony.aloha.factory.ModelParser;
//import com.eharmony.aloha.interop.DoubleFactoryInfo;
//import com.eharmony.aloha.interop.GenericModelParser;
//import com.eharmony.aloha.interop.NoImplModelFactory;
//import com.eharmony.aloha.io.AlohaReadable;
//import com.eharmony.aloha.models.Model;
//import com.eharmony.aloha.models.TopLevelModel;
//import com.eharmony.aloha.models.reg.RegressionModel;
//import com.eharmony.aloha.score.conversions.StrictConversions;
//import com.eharmony.aloha.semantics.compiled.plugin.proto.TestProtoBuffs.TestProto;
//import org.apache.commons.vfs2.FileObject;
//import org.springframework.context.ApplicationContext;
//import org.springframework.context.support.ClassPathXmlApplicationContext;
//import scala.util.Try;
//
//import java.lang.reflect.Method;
//import java.util.Arrays;
//import java.util.Comparator;
//import java.util.List;
//
//
///**
// * mvn clean compile test-compile dependency:copy-dependencies
// * java \
// *   -classpath $(echo "$(find $(pwd)/target/dependency -name '*.jar' | tr '\n' :):$(pwd)/target/classes:$(pwd)/target/test-classes") \
// *   com.eharmony.aloha.semantics.compiled.plugin.proto.SpringFactoryTestMain
// *
// * Exception in thread "main" java.lang.NoSuchMethodError: com.eharmony.aloha.interop.NoImplModelFactory.fromFileObject(Lorg/apache/commons/vfs2/FileObject;)Lscala/util/Try;
// *     at com.eharmony.aloha.semantics.compiled.plugin.proto.SpringFactoryTestMain.main(SpringFactoryTestMain.java:47)
// */
//public final class SpringFactoryTestMain {
//    private SpringFactoryTestMain(){}
//
//    private static final String AppCtxXmlFile = "spring/model.cfg.xml";
//
//    private final ApplicationContext ctx =
//            new ClassPathXmlApplicationContext(AppCtxXmlFile);
//
//    private final AlohaReadable<Try<TopLevelModel<TestProto, Double>>> modelFactory =
//            (AlohaReadable<Try<TopLevelModel<TestProto, Double>>>) ctx.getBean("modelFactory");
//
//    private final NoImplModelFactory<TestProto, Double> codeDefinedFactory =
//            new NoImplModelFactory<TestProto, Double>(
//                    new DoubleFactoryInfo<TestProto>(TestProto.class),
//                    Arrays.<GenericModelParser>asList(RegressionModel.genericParser()));
//
//
//    private final FileObject modelJson = ctx.getBean("modelJson1", FileObject.class);
//
//    private final List<Model<TestProto, Double>> models = (List<Model<TestProto, Double>>) ctx.getBean("models");
//
//    public static void main(String[] args) {
//        SpringFactoryTestMain x = new SpringFactoryTestMain();
//
//        final TestProto p = SpringModelFactoryTest.PROTOS.get(1);
//
//        printMethods();
//
//        for (Model<TestProto, Double> m: x.models) {
//            test(p, m);
//        }
//
//        // NOPE:
////        final TopLevelModel<TestProto, Double> m1 = x.codeDefinedFactory.fromFileObject(x.modelJson).get();
////        test(p, m1, "Model created from factory created in code: ");
//
////        final Model<TestProto, Double> m2 = x.modelFactory.fromFileObject(x.modelJson).get();
////        test(p, m2, "Model created from factory pulled from spring: ");
//    }
//
//    private static void printMethods() {
//        StringBuilder b = new StringBuilder();
//
//        Method[] methods = NoImplModelFactory.class.getDeclaredMethods();
//
//        final Comparator<Method> c = new Comparator<Method>() {
//            @Override
//            public int compare(Method m1, Method m2) {
//                return m1.getName().compareTo(m2.getName());
//            }
//        };
//
//        Arrays.sort(methods, c);
//
//        for (Method m: NoImplModelFactory.class.getDeclaredMethods()) {
//           b.append(", ").append(m.getName());
//        }
//
//        System.out.println("NoImplModelFactory methods: " + b.substring(2));
//
//        int i = 0;
//        for (; i < methods.length && methods[i].getName() != "fromFileObject" ; ++i);
//        final List<Class<?>> paramTypes = Arrays.asList(methods[i].getParameterTypes());
//        System.out.println(methods[i].getName() + "(" + paramTypes.toString() + ") returns " + methods[i].getReturnType());
//    }
//
//    private static void test(TestProto p, Model<TestProto, Double> m) {
//        test(p, m, "");
//    }
//
//    private static void test(TestProto p, Model<TestProto, Double> m, String msg) {
//        System.out.println(msg + " " + m);
//        final double v = StrictConversions.asJavaDouble(m.score(p)).doubleValue();
//        if (Math.abs(SpringModelFactoryTest.EXPECTED_1 - v) > SpringModelFactoryTest.TOLERANCE) {
//            throw new RuntimeException("Expected " + SpringModelFactoryTest.EXPECTED_1 + ", found " + v);
//        }
//    }
//}
