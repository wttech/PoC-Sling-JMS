package com.cognifide.jms.api.consumer;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;



@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
@Documented
public @interface SlingMessageConsumer {

	DestinationType destinationType();

	String subject();
	
	String filter() default "";

	/**
	 * Whether to generate a default SCR component tag. If set to false, a
	 * {@link org.apache.felix.scr.annotations.Component} annotation can be added manually with defined
	 * whatever configuration needed.
	 */
	boolean generateComponent() default true;

	/**
	 * Whether to generate a default SCR service tag with "interface=javax.servlet.Servlet". If set to false,
	 * a {@link org.apache.felix.scr.annotations.Service} annotation can be added manually with defined
	 * whatever configuration needed.
	 */
	boolean generateService() default true;

	/**
	 * Defines the Component name also used as the PID for the Configuration Admin Service. Default value:
	 * Fully qualified name of the Java class.
	 * 
	 * @since 1.6
	 */
	String name() default "";

	/**
	 * Whether Metatype Service data is generated or not. If this parameter is set to true Metatype Service
	 * data is generated in the <code>metatype.xml</code> file for this component. Otherwise no Metatype
	 * Service data is generated for this component.
	 * 
	 * @since 1.6
	 */
	boolean metatype() default false;

	/**
	 * This is generally used as a title for the object described by the meta type. This name may be localized
	 * by prepending a % sign to the name. Default value: %&lt;name&gt;.name
	 * 
	 * @since 1.6
	 */
	String label() default "";

	/**
	 * This is generally used as a description for the object described by the meta type. This name may be
	 * localized by prepending a % sign to the name. Default value: %&lt;name&gt;.description
	 * 
	 * @since 1.6
	 */
	String description() default "";

}
