package se.su.it.svc.ldap

import gldapo.schema.annotation.GldapoSchemaFilter
import gldapo.schema.annotation.GldapoNamingAttribute
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlRootElement
import org.springframework.ldap.core.DistinguishedName
import gldapo.schema.annotation.GldapoSynonymFor
import javax.xml.bind.annotation.XmlElement

/**
 * GLDAPO schema class for SU Service also used by web service.
 */
@XmlAccessorType( XmlAccessType.NONE )
@XmlRootElement
class SuService implements Serializable {

  static final long serialVersionUID = -687991492884005133L;

  @GldapoSchemaFilter("(objectClass=suServiceObject)")

  @GldapoNamingAttribute
  @XmlAttribute
  String cn

  @GldapoSynonymFor("owner")
  @XmlAttribute
  String myowner
  @XmlAttribute
  String suServiceType
  @XmlAttribute
  String suServiceStatus
  @XmlAttribute
  String suServiceStartTime
  @XmlAttribute
  String roleOccupant
  @XmlElement(name="objectClass")
  Set<String> objectClass
}
