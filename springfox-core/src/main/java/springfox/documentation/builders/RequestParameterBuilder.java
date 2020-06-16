package springfox.documentation.builders;

import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import springfox.documentation.schema.Example;
import springfox.documentation.service.ParameterSpecification;
import springfox.documentation.service.ParameterStyle;
import springfox.documentation.service.ParameterType;
import springfox.documentation.service.RequestParameter;
import springfox.documentation.service.VendorExtension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import static springfox.documentation.builders.BuilderDefaults.*;
import static springfox.documentation.builders.NoopValidator.*;

@SuppressWarnings("VisibilityModifier")
public class RequestParameterBuilder {
  //Validator accessible
  String name;
  ParameterType in;
  SimpleParameterSpecificationBuilder simpleParameterBuilder;
  ContentSpecificationBuilder contentSpecificationBuilder;

  private String description;
  private Boolean required = false;
  private Boolean deprecated;
  private Boolean hidden = false;
  private Example scalarExample;
  private final List<Example> examples = new ArrayList<>();
  private final List<VendorExtension> extensions = new ArrayList<>();
  private ParameterSpecificationProvider parameterSpecificationProvider = new RootParameterSpecificationProvider();
  private int precedence;
  private final List<MediaType> accepts = new ArrayList<>();
  private Validator<RequestParameterBuilder> validator = new NoopValidator<>();

  public RequestParameterBuilder name(String name) {
    this.name = defaultIfAbsent(name, this.name);
    return this;
  }

  public RequestParameterBuilder in(ParameterType in) {
    this.in = defaultIfAbsent(in, this.in);
    if (this.in == ParameterType.QUERY || this.in == ParameterType.COOKIE) {
      this.query(q -> q.style(ParameterStyle.FORM)
                       .allowReserved(in == ParameterType.QUERY));
    } else if (this.in == ParameterType.HEADER || this.in == ParameterType.PATH) {
      this.query(q -> q.style(ParameterStyle.SIMPLE)
                       .allowReserved(false));
    }
    return this;
  }

  public RequestParameterBuilder in(String in) {
    if (!StringUtils.isEmpty(in)) {
      this.in = ParameterType.from(in);
    }
    return this;
  }

  public RequestParameterBuilder description(String description) {
    this.description = defaultIfAbsent(description, this.description);
    return this;
  }

  public RequestParameterBuilder required(Boolean required) {
    this.required = defaultIfAbsent(Boolean.TRUE.equals(required) ? true : null, this.required);
    return this;
  }

  public RequestParameterBuilder deprecated(Boolean deprecated) {
    this.deprecated = defaultIfAbsent(deprecated, this.deprecated);
    return this;
  }

  private SimpleParameterSpecificationBuilder queryBuilder() {
    if (simpleParameterBuilder == null) {
      simpleParameterBuilder = new SimpleParameterSpecificationBuilder(this);
    }
    return simpleParameterBuilder;
  }

  public RequestParameterBuilder query(Consumer<SimpleParameterSpecificationBuilder> parameter) {
    parameter.accept(queryBuilder());
    return this;
  }

  private ContentSpecificationBuilder contentBuilder() {
    if (contentSpecificationBuilder == null) {
      contentSpecificationBuilder = new ContentSpecificationBuilder();
    }
    return contentSpecificationBuilder;
  }

  public RequestParameterBuilder content(Consumer<ContentSpecificationBuilder> parameter) {
    parameter.accept(contentBuilder());
    return this;
  }

  public RequestParameterBuilder extensions(List<VendorExtension> extensions) {
    this.extensions.addAll(extensions);
    return this;
  }

  public RequestParameterBuilder hidden(boolean hidden) {
    this.hidden = defaultIfAbsent(hidden, this.hidden);
    return this;
  }

  public RequestParameterBuilder precedence(int precedence) {
    this.precedence = precedence;
    return this;
  }


  public RequestParameterBuilder example(Example scalarExample) {
    this.scalarExample = defaultIfAbsent(scalarExample, this.scalarExample);
    return this;
  }

  public RequestParameterBuilder examples(Collection<Example> examples) {
    this.examples.addAll(examples);
    return this;
  }

  public RequestParameterBuilder parameterSpecificationProvider(ParameterSpecificationProvider specificationProvider) {
    this.parameterSpecificationProvider = specificationProvider;
    return this;
  }

  public RequestParameterBuilder accepts(Collection<? extends MediaType> accepts) {
    this.accepts.addAll(accepts);
    return this;
  }

  public RequestParameterBuilder validator(Validator<RequestParameterBuilder> validator) {
    this.validator = validator;
    return this;
  }

  public RequestParameter build() {
    List<ValidationResult> results = validator.validate(this);
    if (logProblems(results).size() > 0) {
      return null;
    }
    ParameterSpecification parameter = parameterSpecificationProvider.create(
        new ParameterSpecificationContext(
            name,
            in,
            accepts,
            simpleParameterBuilder != null ? simpleParameterBuilder.build() : null,
            contentSpecificationBuilder != null ? contentSpecificationBuilder.build() : null,
            new SimpleParameterSpecificationBuilder(this),
            new ContentSpecificationBuilder()));

    return new RequestParameter(
        name,
        in,
        description,
        in == ParameterType.PATH ? true : required,
        deprecated,
        hidden,
        parameter,
        scalarExample,
        examples,
        precedence,
        extensions);
  }

  public RequestParameterBuilder copyOf(RequestParameter source) {
    source.getParameterSpecification()
          .getQuery()
          .map(simple -> {
            this.query(q -> q.copyOf(simple));
            return simple;
          });
    source.getParameterSpecification()
          .getContent()
          .map(content -> this.content(c -> c.copyOf(content)));
    return this.in(source.getIn())
               .required(source.getRequired())
               .hidden(source.getHidden())
               .deprecated(source.getDeprecated())
               .extensions(source.getExtensions())
               .name(source.getName())
               .description(source.getDescription())
               .precedence(source.getPrecedence())
               .example(source.getScalarExample())
               .examples(source.getExamples());
  }

}