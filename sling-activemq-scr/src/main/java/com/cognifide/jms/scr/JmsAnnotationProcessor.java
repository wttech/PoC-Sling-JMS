package com.cognifide.jms.scr;

import java.util.List;

import javax.jms.MessageListener;

import org.apache.felix.scrplugin.SCRDescriptorException;
import org.apache.felix.scrplugin.SCRDescriptorFailureException;
import org.apache.felix.scrplugin.annotations.AnnotationProcessor;
import org.apache.felix.scrplugin.annotations.ClassAnnotation;
import org.apache.felix.scrplugin.annotations.ScannedClass;
import org.apache.felix.scrplugin.description.ClassDescription;
import org.apache.felix.scrplugin.description.ComponentConfigurationPolicy;
import org.apache.felix.scrplugin.description.ComponentDescription;
import org.apache.felix.scrplugin.description.PropertyDescription;
import org.apache.felix.scrplugin.description.PropertyType;
import org.apache.felix.scrplugin.description.ServiceDescription;

import com.cognifide.jms.api.consumer.DestinationType;
import com.cognifide.jms.api.consumer.MessageConsumerProperties;
import com.cognifide.jms.api.consumer.SlingMessageConsumer;

public class JmsAnnotationProcessor implements AnnotationProcessor {

	public String getName() {
		return "JMS Annotation Processor";
	}

	public void process(final ScannedClass scannedClass, final ClassDescription describedClass)
			throws SCRDescriptorFailureException, SCRDescriptorException {
		final List<ClassAnnotation> servlets = scannedClass.getClassAnnotations(SlingMessageConsumer.class
				.getName());
		scannedClass.processed(servlets);

		for (final ClassAnnotation cad : servlets) {
			processSlingConsumer(cad, describedClass);
		}
	}

	public int getRanking() {
		return 500;
	}

	private void processSlingConsumer(final ClassAnnotation cad, final ClassDescription classDescription) {
		final boolean generateComponent = cad.getBooleanValue("generateComponent", true);
		final boolean metatype = cad.getBooleanValue("metatype", !generateComponent);

		if (generateComponent) {
			final ComponentDescription cd = new ComponentDescription(cad);
			cd.setName(cad.getStringValue("name", classDescription.getDescribedClass().getName()));
			cd.setConfigurationPolicy(ComponentConfigurationPolicy.OPTIONAL);
			cd.setLabel(cad.getStringValue("label", null));
			cd.setDescription(cad.getStringValue("description", null));
			cd.setCreateMetatype(metatype);
			classDescription.add(cd);
		}

		final boolean generateService = cad.getBooleanValue("generateService", true);
		if (generateService) {
			final ServiceDescription sd = new ServiceDescription(cad);
			sd.addInterface(MessageListener.class.getName());
			classDescription.add(sd);
		}

		final DestinationType consumerType = DestinationType.valueOf(cad.getEnumValue("destinationType",
				""));
		if (consumerType != null) {
			final PropertyDescription pd = new PropertyDescription(cad);
			pd.setName(MessageConsumerProperties.DESTINATION_TYPE);
			pd.setValue(consumerType.name());
			pd.setType(PropertyType.String);
			pd.setPrivate(metatype);
			classDescription.add(pd);
		}

		final String subject = (String) cad.getStringValue("subject", "");
		if (subject != null) {
			final PropertyDescription pd = new PropertyDescription(cad);
			pd.setName(MessageConsumerProperties.CONSUMER_SUBJECT);
			pd.setValue(subject);
			pd.setType(PropertyType.String);
			pd.setPrivate(metatype);
			classDescription.add(pd);
		}

		final String filter = (String) cad.getStringValue("filter", "");
		if (filter != null) {
			final PropertyDescription pd = new PropertyDescription(cad);
			pd.setName(MessageConsumerProperties.FILTER);
			pd.setValue(filter);
			pd.setType(PropertyType.String);
			pd.setPrivate(metatype);
			classDescription.add(pd);
		}
	}
}