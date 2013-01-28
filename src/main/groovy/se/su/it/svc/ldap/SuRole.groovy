package se.su.it.svc.ldap

import gldapo.schema.annotation.GldapoNamingAttribute
import gldapo.schema.annotation.GldapoSchemaFilter

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.bind.annotation.XmlElement

/**
 * GLDAPO schema class for SU Role.
 */
@XmlAccessorType( XmlAccessType.NONE )
@XmlRootElement
class SuRole implements Serializable{
  static final long serialVersionUID = -687991493314005073L
  @GldapoSchemaFilter("(objectClass=suRole)")

  @GldapoNamingAttribute
  @XmlAttribute
  String cn
  @XmlElement(name="objectClass")
  Set<String> objectClass
  @XmlElement(name="roleOccupant")
  Set<String> roleOccupant
}
