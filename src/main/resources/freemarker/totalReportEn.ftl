    ${appVersionTitle}

    Total report

Processed root directory "${rootDir}" including subdirectories at ${dateTime}

${renamingInfo}
<#list okFiles as okfile>
${okfile_index + 1}. ${okfile}
</#list>

${renamingErrors}
<#list errFiles as errfile>
${errfile_index + 1}. ${errfile}
</#list>

--
Source code available at https://github.com/fdman85/BrokenImagesDetector