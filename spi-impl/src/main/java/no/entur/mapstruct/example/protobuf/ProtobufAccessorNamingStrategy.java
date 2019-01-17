package no.entur.mapstruct.example.protobuf;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import org.mapstruct.ap.internal.util.Nouns;
import org.mapstruct.ap.spi.DefaultAccessorNamingStrategy;
import org.mapstruct.ap.spi.MapStructProcessingEnvironment;
import org.mapstruct.ap.spi.util.IntrospectorUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Arne Seime
 */
public class ProtobufAccessorNamingStrategy extends DefaultAccessorNamingStrategy {

    public static final String PROTOBUF_MESSAGE_OR_BUILDER = "com.google.protobuf.MessageOrBuilder";
    public static final String PROTOBUF_GENERATED_MESSAGE_V3 = "com.google.protobuf.GeneratedMessageV3";
    public static final String LIST_SUFFIX = "List";
    public static final String BUILDER_LIST_SUFFIX = "BuilderList";

    public static final Set<String> INTERNAL_METHODS = new HashSet<>(
            Arrays.asList("newBuilder", "newBuilderForType", "parseFrom", "parseDelimitedFrom", "getDefaultInstance", "getDescriptor", "getDescriptorForType",
                    "getDefaultInstanceForType", "clear", "clearField", "clearOneof",
                    "mergeFrom", "setRepeatedField", "setUnknownFields", "getSerializedSize", "getAllFields",
                    "getAllFieldsMutable", "getAllFieldsRaw", "getDescriptorForType", "getField", "getFieldRaw", "getOneofFieldDescriptor", "getParserForType",
                    "getRepeatedField", "getRepeatedFieldCount", "getUnknownFields", "getInitializationErrorString", "getMemoizedSerializedSize",
                    "getOneofFieldDescriptor", "getSerializedSize", "getMemoizedSerializedSize", "getSerializingExceptionMessage", "isInitialized",
                    "mergeUnknownFields"
            ));

    public static final List<String> INTERNAL_SPECIAL_METHOD_ENDINGS = Arrays.asList("Value", "Count", "Bytes", "Map", "ValueList");

    public static final List<String> INTERNAL_SPECIAL_METHOD_BEGINNINGS = Arrays.asList("remove", "clear", "mutable", "merge", "putAll", "getMutable", "with");

    protected TypeMirror protobufMesageOrBuilderType;

    @Override
    public void init(MapStructProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);

        TypeElement typeElement = elementUtils.getTypeElement(PROTOBUF_MESSAGE_OR_BUILDER);
        if (typeElement != null) {
            protobufMesageOrBuilderType = typeElement.asType();
        }
    }

    private boolean isSpecialMethod(ExecutableElement method) {
        String methodName = method.getSimpleName().toString();

        for (String checkMethod : INTERNAL_SPECIAL_METHOD_ENDINGS) {
            if (methodName.endsWith(checkMethod)) {
                String propertyMethod = methodName.substring(0, methodName.length() - checkMethod.length());
                boolean propertyMethodExists = method.getEnclosingElement().getEnclosedElements().stream().anyMatch(e -> ((Element) e).getSimpleName().toString().equals(propertyMethod));
                if (propertyMethodExists) {
                    return true;
                }
            }
        }

        for (String checkMethod : INTERNAL_SPECIAL_METHOD_BEGINNINGS) {
            if (methodName.startsWith(checkMethod)) {
                String propertyMethod = "get" + methodName.substring(checkMethod.length());

                boolean propertyMethodExists = method.getEnclosingElement().getEnclosedElements().stream().anyMatch(e -> ((Element) e).getSimpleName().toString().equals(propertyMethod));
                if (propertyMethodExists) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean isGetterMethod(ExecutableElement method) {
        String methodName = method.getSimpleName().toString();

//        if (methodName.endsWith("Builder")) {
//            return false;
//        }

        if (methodName.endsWith("OrBuilder")) {
            return false;
        }

        if (methodName.endsWith("OrBuilderList")) {
            return false;
        }

        if (methodName.endsWith(BUILDER_LIST_SUFFIX)) {
            return false;
        }

        if (INTERNAL_METHODS.contains(methodName)) {
            return false;
        } else {
            if (isSpecialMethod(method)) {
                return false;
            }

            return super.isGetterMethod(method);
        }

    }

    @Override
    public boolean isSetterMethod(ExecutableElement method) {
        String methodName = method.getSimpleName().toString();

        if (INTERNAL_METHODS.contains(methodName)) {
            return false;
        } else {
            if (isSpecialMethod(method)) {
                return false;
            }

            return super.isSetterMethod(method);
        }
    }

    @Override
    protected boolean isFluentSetter(ExecutableElement method) {
        String methodName = method.getSimpleName().toString();

        if (INTERNAL_METHODS.contains(methodName)) {
            return false;
        } else {
            return super.isFluentSetter(method);

        }
    }

    @Override
    public boolean isAdderMethod(ExecutableElement method) {
        String methodName = method.getSimpleName().toString();

        if (INTERNAL_METHODS.contains(methodName)) {
            return false;
        } else {
            return super.isAdderMethod(method);
        }
    }

    @Override
    public boolean isPresenceCheckMethod(ExecutableElement method) {
        String methodName = method.getSimpleName().toString();

        if (INTERNAL_METHODS.contains(methodName)) {
            return false;
        } else {
            return super.isPresenceCheckMethod(method);
        }
    }

    @Override
    public String getElementName(ExecutableElement adderMethod) {

        String methodName = super.getElementName(adderMethod);
        Element receiver = adderMethod.getEnclosingElement();
        if (receiver != null && protobufMesageOrBuilderType != null && typeUtils.isAssignable(receiver.asType(), protobufMesageOrBuilderType)) {
            String singularizedMethodName = Nouns.singularize(methodName);
            methodName = singularizedMethodName;
        }

        return methodName;
    }

    @Override
    public String getPropertyName(ExecutableElement getterOrSetterMethod) {

        String methodName = getterOrSetterMethod.getSimpleName().toString();
        if (methodName.startsWith("get") || methodName.startsWith("set")) {

            if (methodName.endsWith(LIST_SUFFIX)) {
                Element receiver = getterOrSetterMethod.getEnclosingElement();
                if (receiver != null && receiver.getKind() == ElementKind.CLASS) {
                    TypeElement type = (TypeElement) receiver;
                    TypeMirror superType = type.getSuperclass();
                    if (superType != null && superType.toString().startsWith(PROTOBUF_GENERATED_MESSAGE_V3)) {

                        String propertyName = IntrospectorUtils.decapitalize(methodName.substring(3, methodName.length() - 4));

                        return propertyName;
                    }
                }
            }
        }
        return super.getPropertyName(getterOrSetterMethod);
    }

}