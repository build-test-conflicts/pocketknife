package pocketknife.internal.codegen;

import com.squareup.javawriter.JavaWriter;
import pocketknife.internal.BundleBinding;
import pocketknife.internal.GeneratedAdapters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static pocketknife.internal.codegen.BundleFieldBinding.AnnotationType.ARGUMENT;
import static pocketknife.internal.codegen.BundleFieldBinding.AnnotationType.SAVE_STATE;

final class BundleAdapterGenerator {

    private final Set<BundleFieldBinding> fields = new LinkedHashSet<BundleFieldBinding>();
    private final String classPackage;
    private final String className;
    private final String targetType;

    public BundleAdapterGenerator(String classPackage, String className, String targetType) {
        this.classPackage = classPackage;
        this.className = className;
        this.targetType = targetType;
    }

    public void addField(BundleFieldBinding binding) {
        fields.add(binding);
    }

    public void generate(JavaWriter writer) throws IOException {
        writer.emitSingleLineComment(AdapterJavadoc.GENERATED_BY_POCKETKNIFE);
        writer.emitPackage(classPackage);
        writer.emitImports("android.os.Bundle");
        writer.emitEmptyLine();
        writer.beginType(className, "class", EnumSet.of(PUBLIC, FINAL), JavaWriter.type(BundleBinding.class, targetType));
        writer.emitEmptyLine();
        writeBundleKeys(writer);
        // write Save State
        writeSaveState(writer);
        writer.emitEmptyLine();
        // Write restoreState
        writeRestoreState(writer);
        writer.emitEmptyLine();
        // write injectArguments
        writeInjectArguments(writer);
        writer.endType();
    }

    private void writeBundleKeys(JavaWriter writer) throws IOException {
        writer.emitSingleLineComment(AdapterJavadoc.BUNDLE_KEYS);
        for (BundleFieldBinding field : fields) {
            writer.emitField(String.class.getCanonicalName(), field.getKey(), EnumSet.of(PRIVATE, STATIC, FINAL), JavaWriter.stringLiteral(field.getKey()));
        }
    }

    private void writeSaveState(JavaWriter writer) throws IOException {
        writer.emitJavadoc(AdapterJavadoc.SAVE_INSTANCE_STATE_METHOD, targetType);
        writer.beginMethod("void", GeneratedAdapters.SAVE_METHOD, EnumSet.of(PUBLIC), targetType, "target", "Bundle", "bundle");
        for (BundleFieldBinding field : fields) {
            if (SAVE_STATE == field.getAnnotationType()) {
                writeSaveFieldState(writer, field);
            }
        }
        writer.endMethod();
    }

    private void writeSaveFieldState(JavaWriter writer, BundleFieldBinding field) throws IOException {
        writer.emitSingleLineComment(field.getDescription());
        writer.emitStatement("bundle.put%s(%s, target.%s)", field.getBundleType(), field.getKey(), field.getName());
    }

    private void writeRestoreState(JavaWriter writer) throws IOException {
        writer.emitJavadoc(AdapterJavadoc.RESTORE_INSTANCE_STATE_METHOD, targetType);
        writer.beginMethod("void", GeneratedAdapters.RESTORE_METHOD, EnumSet.of(PUBLIC), targetType, "target", "Bundle", "bundle");
        writer.beginControlFlow("if (bundle != null)");
        for (BundleFieldBinding field : fields) {
            if (SAVE_STATE == field.getAnnotationType()) {
                writeRestoreFieldState(writer, field);
            }
        }
        writer.endControlFlow();
        writer.endMethod();
    }

    private void writeRestoreFieldState(JavaWriter writer, BundleFieldBinding field) throws IOException {
        writer.emitSingleLineComment(field.getDescription());
        if (field.isRequired()) {
            writeRequiredRestoreFieldState(writer, field);
        } else {
            writeOptionalRestoreFieldState(writer, field);
        }
    }

    private void writeRequiredRestoreFieldState(JavaWriter writer, BundleFieldBinding field) throws IOException {
        List<String> stmtArgs = new ArrayList<String>();
        writer.beginControlFlow("if (bundle.containsKey(%s))", field.getKey());
        String stmt = "target.".concat(field.getName()).concat(" = ");
        if (field.needsToBeCast()) {
            stmt = stmt.concat(("(%s) "));
            stmtArgs.add(field.getType());
        }
        stmt = stmt.concat("bundle.get%s(%s)");
        stmtArgs.add(field.getBundleType());
        stmtArgs.add(field.getKey());
        writer.emitStatement(stmt, stmtArgs.toArray(new Object[stmtArgs.size()]));
        writer.nextControlFlow("else");
        writer.emitStatement("throw new IllegalStateException(\"Required Bundle value with key '%s' was not found for '%s'. "
                + "If this field is not required add '@NotRequired' annotation\")", field.getKey(), field.getName());
        writer.endControlFlow();
    }

    private void writeOptionalRestoreFieldState(JavaWriter writer, BundleFieldBinding field) throws IOException {
        List<String> stmtArgs = new ArrayList<String>();
        String stmt = "target.".concat(field.getName()).concat(" = ");
        if (field.needsToBeCast()) {
            stmt = stmt.concat("(%s) ");
            stmtArgs.add(field.getType());
        }
        stmt = stmt.concat("bundle.get%s(%s");
        stmtArgs.add(field.getBundleType());
        stmtArgs.add(field.getKey());
        if (field.canHaveDefault()) {
            stmt = stmt.concat(", target.").concat(field.getName());
        }
        stmt = stmt.concat(")");
        writer.emitStatement(stmt, stmtArgs.toArray(new Object[stmtArgs.size()]));
    }

    private void writeInjectArguments(JavaWriter writer) throws IOException {
        writer.emitJavadoc(AdapterJavadoc.INJECT_ARGUMENTS_METHOD, targetType);
        writer.beginMethod("void", GeneratedAdapters.INJECT_ARGUMENTS_METHOD, EnumSet.of(PUBLIC), targetType, "target", "Bundle", "bundle");
        writer.beginControlFlow("if (bundle != null)");
        for (BundleFieldBinding field : fields) {
            if (ARGUMENT == field.getAnnotationType()) {
                writeInjectArgumentField(writer, field);
            }
        }
        writer.endControlFlow();
        writer.endMethod();
    }

    private void writeInjectArgumentField(JavaWriter writer, BundleFieldBinding field) throws IOException {
        writer.emitSingleLineComment(field.getDescription());
        if (field.isRequired()) {
            writeRequiredInjectArgumentField(writer, field);
        } else {
            writeOptionalInjectArgumentField(writer, field);
        }
    }

    private void writeRequiredInjectArgumentField(JavaWriter writer, BundleFieldBinding field) throws IOException {
        List<String> stmtArgs = new ArrayList<String>();
        writer.beginControlFlow("if (bundle.containsKey(%s))", field.getKey());
        String stmt = "target.".concat(field.getName()).concat(" = ");
        if (field.needsToBeCast()) {
            stmt = stmt.concat("(%s) ");
            stmtArgs.add(field.getType());
        }
        stmt = stmt.concat("bundle.get%s(%s)");
        stmtArgs.add(field.getBundleType());
        stmtArgs.add(field.getKey());
        writer.emitStatement(stmt, stmtArgs.toArray(new Object[stmtArgs.size()]));
        writer.nextControlFlow("else");
        writer.emitStatement("throw new IllegalStateException(\"Required Argument with key '%s' was not found for '%s'. "
                + "If this field is not required add '@NotRequired' annotation\")", field.getKey(), field.getName());
        writer.endControlFlow();

    }

    private void writeOptionalInjectArgumentField(JavaWriter writer, BundleFieldBinding field) throws IOException {
        List<String> stmtArgs = new ArrayList<String>();
        String stmt = "target.".concat(field.getName()).concat(" = ");
        if (field.needsToBeCast()) {
            stmt = stmt.concat("(%s) ");
            stmtArgs.add(field.getType());
        }
        stmt = stmt.concat("bundle.get%s(%s");
        stmtArgs.add(field.getBundleType());
        stmtArgs.add(field.getKey());
        if (field.canHaveDefault()) {
            stmt = stmt.concat(", target.").concat(field.getName());
        }
        stmt = stmt.concat(")");
        writer.emitStatement(stmt, stmtArgs.toArray(new Object[stmtArgs.size()]));
    }

    public CharSequence getFqcn() {
        return classPackage + "." + className;
    }
}